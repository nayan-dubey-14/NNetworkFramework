import java.io.*;
import java.net.*;
import java.lang.annotation.*;
import java.lang.reflect.*;
import java.util.*;

//Custom Annotation
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE,ElementType.METHOD})
@interface Path
{
public String value();
}

class RequestProcessor extends Thread
{
private Socket socket;
public RequestProcessor(Socket socket)
{
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

//deserialized
ByteArrayInputStream bais=new ByteArrayInputStream(request);
ObjectInputStream ois=new ObjectInputStream(bais);
Request clientRequest=(Request)ois.readObject();

//the below we get from Request class object
String path=clientRequest.path;
Object arguments[]=clientRequest.arguments;

int index=path.indexOf("/",1);
String userClassAnnotationValue=path.substring(1,index);
String userMethodAnnotationValue=path.substring(index);

Class userClass=null;
String userClassName=null;
Method userClassMethod=null;
String userMethodName=null;
//checking if class has annotation applied and of Path type then it is our target class
for(int itr=0;itr<NFrameworkServer.classList.size();itr++)
{
userClass=NFrameworkServer.classList.get(itr);
if(userClass.isAnnotationPresent(Path.class))
{
Path p=(Path)userClass.getAnnotation(Path.class);
if(p.value().equals(userClassAnnotationValue))
{
userClassName=userClass.getName();
break;
}
}
}
if(userClass!=null)
{
//checking if annotation is applied on method and that is of Path type then it is our target
Method userClassMethods[]=userClass.getMethods();
for(Method method:userClassMethods)
{
if(method.isAnnotationPresent(Path.class))
{
Path pa=(Path)method.getAnnotation(Path.class);
if(pa.value().equals(userMethodAnnotationValue))
{
userClassMethod=method;
userMethodName=method.getName();
break;
}
}
}
}

//invoke the method for that target class ,which client wants to invoke
Object userClassObject=userClass.newInstance();
Object result=userClassMethod.invoke(userClassObject,arguments);


ByteArrayOutputStream baos=new ByteArrayOutputStream();
ObjectOutputStream oos=new ObjectOutputStream(baos);
oos.writeObject(result);
oos.flush();
byte []objectArray=baos.toByteArray();

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
System.out.println("header ready");
//sent header to client and then receive acknowledgment
os.write(h,0,1024);
os.flush();
System.out.println("header sent");
while(true)
{
bytesReadCount=is.read(ack);
if(bytesReadCount==-1) continue;
break;
}
System.out.println("ack recieved");

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
System.out.println("Response sent ");
socket.close();
}catch(Exception e)
{
System.out.println(e);
}
}
}


class NFrameworkServer
{
private ServerSocket serverSocket;
private Socket socket;
public static ArrayList<Class> classList=new ArrayList<>();
public NFrameworkServer()
{
}
void registerClass(Class c)
{
classList.add(c);
}
public void start()
{
try
{
serverSocket=new ServerSocket(6060);
}catch(IOException ioException)
{
System.out.println(ioException);
}
startListening();
}
private void startListening()
{
while(true)
{
try
{
System.out.println("Server is ready to accept request at PORT 6060");
socket=serverSocket.accept();
RequestProcessor rp=new RequestProcessor(socket);
}catch(Exception exception)
{
System.out.println(exception);
}
}
}
}
