package com.thinking.machines.nframework.client;
import com.thinking.machines.nframework.common.*;
import java.nio.charset.*;
import java.net.*;
import java.io.*;
public class NFrameworkClient
{
public Object execute(String servicePath,Object ...arguments) throws Throwable
{
try
{
//Serialize
Request requestObject=new Request();
requestObject.setServicePath(servicePath);
requestObject.setArguments(arguments);
String requestJSONString=JSONUtil.toJSON(requestObject);
byte objectArray[]=requestJSONString.getBytes(StandardCharsets.UTF_8);

//create header and set length of objectArray in it
int requestLength=objectArray.length;
byte header[]=new byte[1024];
int x=requestLength;
int j=1023;
while(x>0)
{
header[j]=(byte)(x%10);
x=x/10;
j--;
}

//sent header to server and then receive acknowledgment
Socket socket=new Socket("localhost",6060);
OutputStream os=socket.getOutputStream();
os.write(header,0,1024);
os.flush();
//System.out.println("Header sent");
InputStream is=socket.getInputStream();
byte ack[]=new byte[1];
int bytesReadCount;
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}

//now sent request(byte array) in chunks of 1024
int bytesToSend=requestLength;
int chunkSize=1024;
j=0;
while(j<bytesToSend)
{
if(bytesToSend-j<chunkSize) chunkSize=bytesToSend-j;
os.write(objectArray,j,chunkSize);
os.flush();
j+=chunkSize;
}
//System.out.println("Request sent");

//now receive header and take out the length of upcoming response
int bytesToReceive=1024;
byte tmp[]=new byte[1024];
int k,i;
i=j=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
//System.out.println("brc : "+bytesReadCount);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
header[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}
//System.out.println("header received");
int responseLength=0;
i=1;
j=1023;
while(j>=0)
{
responseLength=responseLength+(header[j]*i);
i=i*10;
j--;
}
//System.out.println("Response length : "+responseLength);
ack[0]=1;
os.write(ack,0,1);
os.flush();

//now receive the response 
byte response[]=new byte[responseLength];
bytesToReceive=responseLength;
i=j=0;
while(j<bytesToReceive)
{
bytesReadCount=is.read(tmp);
if(bytesReadCount==-1) continue;
for(k=0;k<bytesReadCount;k++)
{
response[i]=tmp[k];
i++;
}
j+=bytesReadCount;
}
//System.out.println("Response received");
ack[0]=1;
os.write(ack,0,1);
os.flush();
socket.close();

//deserialized
String responseJSONString=new String(response,StandardCharsets.UTF_8);
Response responseObject=JSONUtil.fromJSON(responseJSONString,Response.class);
if(responseObject.getSuccess())
{
return responseObject.getResult();
}
else
{
throw responseObject.getException();
}
}catch(Exception e)
{
System.out.println(e.getMessage());
}
return null;
}
}