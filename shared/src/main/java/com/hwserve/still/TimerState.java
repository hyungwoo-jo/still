package com.hwserve.still;

/** Pure state machine. All time arguments use a monotonic clock. */
public final class TimerState {
 public int mode=0, completed=0, cycle=4;
 public long remaining=25*60_000L, duration=remaining, deadline=0;
 public boolean running=false;
 public long left(long now) { return running ? Math.max(0,deadline-now) : remaining; }
 public void toggle(long now) { if(running) { remaining=left(now); running=false; } else { deadline=now+remaining; running=true; } }
 public boolean due(long now) { return running && now>=deadline; }
 public boolean advance(boolean finished, long now, long focus, long shortBreak, long longBreak, boolean autoFocus, boolean autoBreak) {
  boolean credit=finished && mode==0;
  if(mode==0) { if(credit) completed++; mode=completed>=cycle ? 2 : 1; }
  else { if(mode==2) completed=0; mode=0; }
  duration=mode==0?focus:mode==1?shortBreak:longBreak; remaining=duration;
  running=finished && (mode==0?autoFocus:autoBreak); deadline=now+duration;
  return credit;
 }
 public void reset() { remaining=duration; running=false; deadline=0; }
 /** Reset and boot must not count a due interval. Every other arrival does. */
 public static boolean completesOnArrival(String command, boolean due) {
  return due && !"reset".equals(command) && !"boot".equals(command);
 }
 /** Remaining time after reboot. Wall-clock deadline is the only clock that survives it. */
 public static final class BootRecovery {
  public final long remaining;
  public final boolean complete;
  public BootRecovery(long remaining, boolean complete) { this.remaining=remaining; this.complete=complete; }
  public static BootRecovery decide(long wallNow, long wallDeadline, long duration, long checkpoint) {
   if (wallDeadline<=0) return new BootRecovery(checkpoint, false);
   if (wallNow<wallDeadline) {
    long left=wallDeadline-wallNow;
    if (left>duration) left=duration;
    return new BootRecovery(left, false);
   }
   return new BootRecovery(0, true);
  }
 }
}
