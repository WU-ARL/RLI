import java.lang.reflect.Array;
import java.lang.*;//String;
///////////////////////////////////////////////// NextHopIP ////////////////////////////////////////////////////////////

public class NextHopIP extends NextHop
{
  public NextHopIP() {super();}
  public NextHopIP(String s) throws java.text.ParseException
  {
    super(s);
  }
  public NextHopIP(NextHopIP nh) { super((NextHop)nh);}
  public NextHopIP(int p, Hardware hwi) { super(p, hwi);}
}
