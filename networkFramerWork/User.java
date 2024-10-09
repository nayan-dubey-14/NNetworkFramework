import java.io.*;
public class User
{
public static void main(String gg[])
{
NFrameworkClient nfc=new NFrameworkClient();
Object result=nfc.process("/calculator/sub",100,30);
System.out.println("Answer : "+(Integer)result);
}
}