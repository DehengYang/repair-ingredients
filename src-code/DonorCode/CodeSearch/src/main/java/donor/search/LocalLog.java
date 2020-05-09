package donor.search;

public class LocalLog {

    public static void log(String log){
        StackTraceElement[] stacks = new Throwable().getStackTrace();
        StackTraceElement stack1 = stacks[1];
        System.out.println("---LOG POINT@---\n"
//                + "filename: "+stack1.getFileName()
                + "classname:"+stack1.getClassName()
                + "  methodname:"+stack1.getMethodName()
                + "  lineno:"+stack1.getLineNumber());
        System.out.println("---LOG INFO:---\n"+log);
    }

//    public static void main(String[] args){
//        LocalLog.log("this is a test!");
//    }
}