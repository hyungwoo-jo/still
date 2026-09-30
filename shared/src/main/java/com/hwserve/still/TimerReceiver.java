package com.hwserve.still;
import android.content.*;
public final class TimerReceiver extends BroadcastReceiver {
 @Override public void onReceive(Context c,Intent i) {
  String a=i.getAction();
  TimerEngine e=new TimerEngine(c);
  if(Intent.ACTION_BOOT_COMPLETED.equals(a)) { e.state.running=false; e.command("boot"); }
  else e.command(Intent.ACTION_MY_PACKAGE_REPLACED.equals(a)?"tick":a);
 }
}
