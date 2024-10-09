package com.thinking.machines.nframework.server;
import com.thinking.machines.nframework.common.*;
import java.io.*;
import java.lang.reflect.*;
import java.nio.charset.*;
import java.net.*;
class RequestProcessor extends Thread
{
private NFrameworkServer server;
private Socket socket;
public RequestProcessor(NFrameworkServer server,Socket socket)
{
this.server=server;
this.socket=socket;
start();
}
public void run()
{
try
{
//now receive header and take out the length of upcoming request
int bytesToReceive=1024;
int bytesReadCount;
byte ack[]=new byte[1];
byte tmp[]=new byte[1024];
byte header[]=new byte[1024];
int k,i,j;
i=j=0;
InputStream is=socket.getInputStream();
OutputStream os=socket.getOutputStream();

while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}
int requestLength=0;
i=1;
j=1023;
while(j>=0)
{
requestLength=requestLength+(header[j]*i);
i=i*10;
j--;
}
ack[0]=1;
os.write(ack,0,1);
os.flush();

//now receive the request 
byte request[]=new byte[requestLength];
bytesToReceive=requestLength;
i=j=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
request[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}

String requestJSONString=new String(request,StandardCharsets.UTF_8);
Request requestObject=JSONUtil.fromJSON(requestJSONString,Request.class);
String servicePath=requestObject.getServicePath();
TCPService tcpService=this.server.getTCPService(servicePath);
Response responseObject=new Response();
if(tcpService==null)
{
responseObject.setSuccess(false);
responseObject.setResult(null);
responseObject.setException(new RuntimeException("Invalid path : "+servicePath));
}
else
{
Class c=tcpService.c;
Method method=tcpService.method;
try
{
Object serviceObject=c.newInstance();
Object result=method.invoke(serviceObject,requestObject.getArguments());
responseObject.setSuccess(true);
responseObject.setResult(result);
responseObject.setException(null);
}catch(InstantiationException instantiationException)
{
responseObject.setSuccess(false);
responseObject.setResult(null);
responseObject.setException(new RuntimeException("Unable to create object to service class associated with the path : "+servicePath));
}
catch(IllegalAccessException illegalAccessException)
{
responseObject.setSuccess(false);
responseObject.setResult(null);
responseObject.setException(new RuntimeException("Unable to create object to service class associated with the path : "+servicePath));
}
catch(InvocationTargetException invocationTargetException)
{
//this change we have made
Throwable cause=invocationTargetException.getCause();
responseObject.setSuccess(false);
responseObject.setResult(null);
responseObject.setException(cause);
}
}

String responseJSONString=JSONUtil.toJSON(responseObject);
byte []objectArray=responseJSONString.getBytes(StandardCharsets.UTF_8);
//create header and set length of objectArray in it
int responseLength=objectArray.length;
int x=responseLength;
j=1023;
byte h[]=new byte[1024];
while(x>0)
{
h[j]=(byte)(x%10);
x=x/10;
j--;
}
//System.out.println("header ready");
//sent header to client and then receive acknowledgment
os.write(h,0,1024);
os.flush();
//System.out.println("header sent");
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
//System.out.println("ack recieved");

//now sent response(byte array) in chunks of 1024
int bytesToSend=responseLength;
int chunkSize=1024;
j=0;
while(j<bytesToSend)
{
if(bytesToSend-j<chunkSize) chunkSize=bytesToSend-j;
os.write(objectArray,j,chunkSize);
os.flush();
j+=chunkSize;
}
//System.out.println("Response sent");
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
//System.out.println("ack recieved");
socket.close();
}catch(IOException e)
{
System.out.println(e);
}
}
}