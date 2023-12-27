## rpc与mapreduce

### 项目简介

本项目基于netty实现通用的rpc框架，rpc分别实现了服务端与客户端，服务由接口+类的形式提供，服务端需要实现服务接口并将实现类注册到本地注册中心，此外，服务端还需要将提供的服务名称（类.方法）注册到远程注册中心zookeeper，客户端通过服务端的注册中心拿到提供服务的所有server信息，通过netty进行通信拿到服务端的执行结果，客户端提供代理模式供选择，代理接口直接提供了访问远程客户端的方法封装，直接调用就可获得服务对象，直接调用本地方法就可以实现rpc远程调用。在实现rpc的基础上，逐渐开启mit6.824的学习。该项目在实现上比较简化，只追求跑通理解概念，大体上比较容易理解，许多细节还有思考的空间。

### 使用方式

客户端和服务端应该知道他们的通信方式和内容，这里让客户端和服务端都拥有一个公共的接口方便客户端可以直接调用，本质上这里可以使用不同的数据交换协议，例如protobuf等

服务端

```
 //开启一个server，指定ip端口提供服务（这里使用ServerPool管理server，也可以直接创建）
 RpcServer rpcServer = ServerPool.get("127.0.0.1:8083");
 //注册服务，第一个参数是服务端提供的对应接口的具体实现类，第二个参数是提供服务的对象实例，这里new是让服务过程中不重复new对象，第三个参数代表提供服务的具体方法
 rpcServer.register(Echo.class, new Echo(), "echo");
 //启动服务
 rpcServer.start();
```

客户端

```
//使用BeanProxy直接代理实现对应接口，代理方法请求了远程server服务
Message echo = (Message)BeanProxy.getBean(Message.class, "Echo");
//直接调用就可获取服务端执行结果
String res = echo.echo("huachuan");
System.out.println(res);
```



### 项目架构

#### rpc架构

!<img src=".\images\rpc.png" alt="rpc" />

上图可以描述为如下几个步骤

- server端向本地注册中心添加服务，添加服务后server会将该服务同步到zookeeper注册中心，zookeeper中的组织结构方式如上图所示。

- 客户端在初始化时，会启动一个本地缓存去获取zookeeper的注册信息，避免每次请求都会请求zookeeper，通过注册watcher，当zookeeper注册新的服务或者发生改变时客户端能够及时感知并更新本地缓存。

- 客户端请求服务时，首先去获取缓存中的服务注册信息，若获取失败，则说明没有提供该服务的server，获取成功后可以自定义负载均衡策略获取server，本实现只简单地随机获取server进行处理。

- 客户端可以使用实现的BeanProxy工具类获取服务的代理实现，代理实现封装了客户端调用服务的一系列过程。

  

#### mapreduce框架

mapreduce主要通过map和reduce两个操作来完成一个复杂的任务，其主要通过master和多个worker组成，master负责分发任务和错误处理，worker负责具体任务的完成（map or reduce)，本实现的map和reduce的任务是完成文件中的单词统计。

<img src=".\images\mapReduce.png" alt="rpc" style="zoom:50%;" />

在master启动阶段，需要告知master需要处理的文件夹路径（默认在resources/wordCountFiles/），在启动后，master需要不断监听以此来连接worker，当有worker连接后便可以给其分发任务，在worker工作的整个过程中，master会不断地发送心跳检测以此来判断当前worker的健康状况，如果当前worker不能正常响应，master就会主动断开连接(当然，这里可以有很多的判定策略)。worker在初始化时，会开启能够供master调用的本地服务，当worker初始化完成后，就会主动与master发起连接请求，连接成功后就可以正常提供服务了。

在跨机器的处理时，非常容易因为网络故障或者机器宕机而造成分发的任务不能按时完成，从而造成系统的可靠性降低，所以容错处理是非常必要的，master作为整个系统的协调者，应该需要考虑很多情况：

1.worker断开连接或者无响应怎么办？

master只要未得到worker的响应结果，会等待至少一个心跳周期，若在该周期内worker主动断开连接或者周期心跳无反应，master感知后会主动断开连接并将该任务重新放回任务队列进行再次分配。

2.如何保证任务的有序发放，做到不丢失，不重复？

在任务的发放过程中分层次发放，先分发map任务，在分发reduce任务，这样可以减少并发带来的效率问题。由于master的任务确认机制，不丢失是能够做到的，但是不重复需要通过额外的机制进行实现，比如map完成后进行去重等，本项目中的任务为统计单词个数，可以通过文件系统自身的机制实现不重复。

3.多线程并发问题？

mapreduce中的线程并发问题主要集中在对worker资源以及任务资源的并发访问上，在分配worker和任务时，均使用了锁来实现有序并发（对于任务多的场景可以使用CAS和预分配策略）。

### 技术要点

- netty通信
- zookeeper注册中心
- 动态代理 
- 多线程同步
- maven分模块
- git操作（设置.gitignore文件）

