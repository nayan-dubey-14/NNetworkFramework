import java.net.*;
import java.io.*;
class NFrameworkClient
{
private Socket socket;
public NFrameworkClient()
{
try
{
socket=new Socket("localhost",6060);
}catch(Exception exception)
{
}
}
//Object ...args (variable argument ,means it can take n arguments)
public Object process(String path,Object ...args)
{
try
{
Request clientRequest=new Request();
clientRequest.path=path;
clientRequest.arguments=new Object[args.length];
for(int itr=0;itr<args.length;itr++)
{
clientRequest.arguments[itr]=args[itr];
}

//Serialize
ByteArrayOutputStream baos=new ByteArrayOutputStream();
ObjectOutputStream oos=new ObjectOutputStream(baos);
oos.writeObject(clientRequest);  //request class object send();
oos.flush();
byte objectArray[]=baos.toByteArray();

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
OutputStream os=socket.getOutputStream();
os.write(header,0,1024);
os.flush();

System.out.println("Header sent");
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
System.out.println("Request sent");
//now receive header and take out the length of upcoming response
int bytesToReceive=1024;
byte tmp[]=new byte[1024];
int k,i;
i=j=0;
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
System.out.println("header received");
int responseLength=0;
i=1;
j=1023;
while(j>=0)
{
responseLength=responseLength+(header[j]*i);
i=i*10;
j--;
}
System.out.println("Response length : "+responseLength);
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
System.out.println("Response received");
ack[0]=1;
os.write(ack,0,1);
os.flush();
socket.close();
//deserialized
ByteArrayInputStream bais=new ByteArrayInputStream(response);
ObjectInputStream ois=new ObjectInputStream(bais);
return (Object)ois.readObject();
}catch(Exception exception)
{
System.out.println(exception.getMessage());
}
return 0;
}
}