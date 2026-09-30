package com.hwserve.still;
import android.app.*;
import android.content.*;
import android.os.*;
import android.graphics.Bitmap;
import android.view.*;
import android.widget.*;
import com.hwserve.still.phone.R;
import java.io.FileOutputStream;

/** Exercises the actual RemoteViews layout and its PendingIntent on Android. */
public final class WidgetProbe extends Instrumentation {
 private View widget;
 @Override public void onCreate(Bundle args){super.onCreate(args);start();}
 @Override public void onStart(){
  Bundle result=new Bundle();
  try {
   Context c=getTargetContext();
   Activity a=startActivitySync(new Intent(c,MainActivity.class).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
   runOnMainSync(()->{
    TimerEngine e=new TimerEngine(c);e.state.mode=0;e.state.completed=0;e.state.running=false;e.state.duration=1500000;e.state.remaining=1500000;e.command("tick");
    FrameLayout frame=new FrameLayout(a);frame.setBackgroundColor(0xfffafbf8);a.setContentView(frame);
    widget=StillWidget.createViews(c).apply(a,frame);
    float density=a.getResources().getDisplayMetrics().density;
    FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams((int)(300*density),(int)(160*density),Gravity.CENTER);frame.addView(widget,lp);
    if(widget.findViewById(R.id.widget_time).getVisibility()!=View.VISIBLE)throw new AssertionError("Paused time missing");
    if(!"25:00".contentEquals(((TextView)widget.findViewById(R.id.widget_time)).getText()))throw new AssertionError("Paused time incorrect");
    widget.findViewById(R.id.widget_toggle).performClick();
   });
   SystemClock.sleep(500);
   if(!new TimerEngine(c).state.running)throw new AssertionError("Widget start did not dispatch");
   runOnMainSync(()->{
    StillWidget.createViews(c).reapply(a,widget);
    if(widget.findViewById(R.id.widget_clock).getVisibility()!=View.VISIBLE)throw new AssertionError("Running countdown missing");
    Chronometer clock=widget.findViewById(R.id.widget_clock);
    if(!clock.isCountDown())throw new AssertionError("Chronometer must count down");
   });
   SystemClock.sleep(1000);
   Bitmap capture=getUiAutomation().takeScreenshot();
   try(FileOutputStream out=c.openFileOutput("widget-preview.png",0)){capture.compress(Bitmap.CompressFormat.PNG,100,out);}
   runOnMainSync(()->widget.findViewById(R.id.widget_toggle).performClick());SystemClock.sleep(500);
   if(new TimerEngine(c).state.running)throw new AssertionError("Widget pause did not dispatch");
   result.putString("stream","PASS: widget RemoteViews inflation, paused text, start PendingIntent, live countdown, pause PendingIntent\n");finish(Activity.RESULT_OK,result);
  } catch(Throwable error){result.putString("stream","FAIL: "+error.toString());finish(Activity.RESULT_CANCELED,result);}
 }
}
