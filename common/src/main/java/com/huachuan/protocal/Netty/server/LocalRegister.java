package com.huachuan.protocal.Netty.server;

import com.huachuan.entity.Url;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class LocalRegister{


    private Map<String, Class> map = new ConcurrentHashMap<>();

    //本地注册

    /**
     *
     * @param serverName 类中的方法名称
     * @param cls 类类型
     */
    public void register(Class cls, String serverName, String address, int port) throws Exception {
        String sn = cls.getSimpleName() + "." + serverName;
        if(map.containsKey(sn)) {
            System.out.println("服务" + sn + "已经存在");
            return;
        }
        map.put(sn,cls);
        ZookeeperRegisterImpl.register(sn, new Url(address, port));
    }

    //本地移除服务
    public void remove(String className, String serverName) {
        String sn = className + "." + serverName;
        map.remove(sn);
    }

    //获取服务的类类型
    public Class get(String className, String serverName) {
        String sn = className + "." + serverName;
        return map.get(sn);
    }


}
