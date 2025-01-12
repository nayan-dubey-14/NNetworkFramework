package com.thinking.machines.nframework.server;
import com.thinking.machines.nframework.server.annotations.*;
import java.lang.reflect.*;
import java.io.*;
import java.net.*;
import java.util.*;
public class NFrameworkServer
{
private ServerSocket serverSocket;
private Set<Class> tcpNetworkServiceClasses;
private Map<String,TCPService> services;
public NFrameworkServer()
{
tcpNetworkServiceClasses=new HashSet<>();
services=new HashMap<>();
}
public void registerClass(Class c)
{
//if it is already registered
if(tcpNetworkServiceClasses.contains(c)) return;
int methodWithPathAnnotationCount=0;
Path pathOnType;
Path pathOnMethod;
Method methods[];
String fullPath;
TCPService tcpService=null;
pathOnType=(Path)c.getAnnotation(Path.class);
if(pathOnType==null) return;
methods=c.getMethods();
for(Method method:methods)
{
pathOnMethod=(Path)method.getAnnotation(Path.class);
if(pathOnMethod==null) continue;
fullPath=pathOnType.value()+pathOnMethod.value();
tcpService=new TCPService();
tcpService.c=c;
tcpService.method=method;
tcpService.path=fullPath;
services.put(fullPath,tcpService);
methodWithPathAnnotationCount++;
}
if(methodWithPathAnnotationCount>0)
{
tcpNetworkServiceClasses.add(c);
}
}
public TCPService getTCPService(String path)
{
return services.get(path);
}
public void start()
{
try
{
serverSocket=new ServerSocket(6060);
Socket socket;
RequestProcessor requestProcessor;
while(true)
{
socket=serverSocket.accept();
requestProcessor=new RequestProcessor(this,socket);
}
}catch(Exception exception)
{
System.out.println(exception.getMessage());
}
}
}