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
 check(s.left(Long.MAX_VALUE)==1500,"paused state ignores clock");
 TimerState.BootRecovery ahead=TimerState.BootRecovery.decide(1_000,1_000+600_000,1_500_000,1_500_000);
 check(ahead.remaining==600_000&&!ahead.complete,"reboot keeps remaining time until the wall deadline");
 TimerState.BootRecovery done=TimerState.BootRecovery.decide(2_000,1_000,1_500_000,1_500_000);
 check(done.complete&&done.remaining==0,"reboot completes an interval that elapsed while off");
 TimerState.BootRecovery idle=TimerState.BootRecovery.decide(5_000,0,1_500_000,400_000);
 check(!idle.complete&&idle.remaining==400_000,"reboot without a wall deadline uses the checkpoint");
 TimerState.BootRecovery skewed=TimerState.BootRecovery.decide(1_000,1_000+9_000_000,1_500_000,1_500_000);
 check(!skewed.complete&&skewed.remaining==1_500_000,"reboot remaining is clamped to the phase duration");
 check(!TimerState.completesOnArrival("reset",true),"reset does not complete a due interval");
 check(!TimerState.completesOnArrival("boot",true),"boot does not use the stale elapsed deadline");
 check(TimerState.completesOnArrival("tick",true)&&TimerState.completesOnArrival("end",true),"tick and alarm complete a due interval");
 check(!TimerState.completesOnArrival("toggle",false),"toggle before the deadline does not complete");
 check(TimerState.completesOnArrival("skip",true),"skip at zero still completes");
 System.out.println("PASS: "+checks+" timer assertions");
 }
}