package chap12;
import Stone.ClassParser;
import Stone.ParseException;
import chap11.EnvOptInterpreter;
import chap11.ResizableArrayEnv;
import chap8.Natives;

public class ObjOptInterpreter extends EnvOptInterpreter {
    public static void main(String[] args) throws ParseException {
        run(new ClassParser(),
            new Natives().environment(new ResizableArrayEnv()));
    }
}