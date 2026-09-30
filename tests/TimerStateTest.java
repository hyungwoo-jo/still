import com.hwserve.still.TimerState;
public class TimerStateTest {
 static int checks=0;
 static void check(boolean ok,String why){checks++;if(!ok)throw new AssertionError(why);}
 static boolean advance(TimerState s,boolean done,long now,boolean af,boolean ab){return s.advance(done,now,1500,300,900,af,ab);}
 public static void main(String[] args){
 TimerState s=new TimerState();s.duration=1500;s.remaining=1500;
 s.toggle(100);check(s.left(500)==1100,"countdown uses elapsed time");s.toggle(500);check(!s.running&&s.remaining==1100,"pause freezes remainder");s.toggle(9000);check(s.deadline==10100,"resume excludes paused time");check(!s.due(10099)&&s.due(10100),"exact endpoint");
 check(advance(s,true,10100,false,false),"completed focus credited");check(s.mode==1&&s.completed==1&&!s.running&&s.remaining==300,"short break waits by default");
 check(!advance(s,false,11000,true,true)&&s.mode==0&&!s.running,"skip break never autostarts");
 advance(s,false,12000,true,true);check(s.completed==1&&s.mode==1,"skip focus gives no credit");advance(s,false,12000,false,false);
 for(int i=0;i<3;i++){advance(s,true,12000,false,true);if(i<2)advance(s,true,13000,true,false);}
 check(s.mode==2&&s.completed==4&&s.duration==900,"four completed focuses select long break");check(s.running,"break auto start");advance(s,true,14000,true,false);check(s.mode==0&&s.completed==0&&s.running,"long break resets cycle and auto starts focus");s.toggle(14200);s.reset();check(!s.running&&s.remaining==1500&&s.completed==0,"reset restores current duration");
 s.cycle=2;advance(s,true,15000,false,false);advance(s,true,16000,false,false);advance(s,true,17000,false,false);check(s.mode==2,"custom cycle");advance(s,false,18000,true,true);check(s.mode==0&&s.completed==0&&!s.running,"skip long break clears cycle without auto start");
 check(s.left(Long.MAX_VALUE)==1500,"paused state ignores clock");System.out.println("PASS: "+checks+" timer assertions");
 }
}