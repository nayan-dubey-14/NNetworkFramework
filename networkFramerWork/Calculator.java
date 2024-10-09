import java.io.*;
@Path("/calculator")
public class Calculator
{
@Path("/add")
public int add(int x,int y)
{
return x+y;
}
@Path("/sub")
public int subtract(int x,int y)
{
return x-y;
}
public static void main(String gg[])
{
NFrameworkServer nf=new NFrameworkServer();
nf.registerClass(Calculator.class);
nf.start();
}
}