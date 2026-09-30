package com.hwserve.still;
import com.hwserve.still.phone.R;
import android.appwidget.*;
import android.content.*;
import android.os.SystemClock;
import android.view.View;
import android.widget.RemoteViews;
public final class StillWidget extends AppWidgetProvider {
 public static void updateAll(Context c) { AppWidgetManager m=AppWidgetManager.getInstance(c); render(c,m,m.getAppWidgetIds(new ComponentName(c,StillWidget.class))); }
 @Override public void onUpdate(Context c,AppWidgetManager m,int[] ids) { new TimerEngine(c).command("tick"); render(c,m,ids); }
 private static void render(Context c,AppWidgetManager m,int[] ids) {
  for(int id:ids) m.updateAppWidget(id,createViews(c));
 }
 public static RemoteViews createViews(Context c) {
   TimerEngine e=new TimerEngine(c);
   RemoteViews v=new RemoteViews(c.getPackageName(),R.layout.still_widget);
   v.setTextViewText(R.id.widget_label,"STILL · "+e.label()); v.setTextViewText(R.id.widget_time,TimerEngine.format(e.state.left(SystemClock.elapsedRealtime())));
   v.setViewVisibility(R.id.widget_time,e.state.running?View.GONE:View.VISIBLE); v.setViewVisibility(R.id.widget_clock,e.state.running?View.VISIBLE:View.GONE);
   v.setChronometerCountDown(R.id.widget_clock,true); v.setChronometer(R.id.widget_clock,e.state.deadline,null,e.state.running);
   StringBuilder dots=new StringBuilder(); for(int j=0;j<e.state.cycle;j++) dots.append(j<e.state.completed?"● ":"○ ");
   v.setTextViewText(R.id.widget_dots,dots.toString()); v.setTextViewText(R.id.widget_toggle,e.state.running?"일시정지":"시작");
   v.setOnClickPendingIntent(R.id.widget_root,TimerEngine.open(c)); v.setOnClickPendingIntent(R.id.widget_toggle,TimerEngine.action(c,"toggle")); return v;
 }
}
