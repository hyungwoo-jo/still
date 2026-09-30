package com.hwserve.still;
import com.hwserve.still.shared.R;

import android.app.*;
import android.content.*;
import android.os.*;
import org.json.*;
import java.time.*;
import java.util.Locale;

public final class TimerEngine {
 public static final String CHANGED="com.hwserve.still.CHANGED";
 public final Context context;
 public final android.content.SharedPreferences prefs;
 public final TimerState state=new TimerState();
 public TimerEngine(Context c) {
  context=c.getApplicationContext(); prefs=context.getSharedPreferences("still",0);
  state.mode=prefs.getInt("mode",0); state.completed=prefs.getInt("completed",0); state.cycle=prefs.getInt("cycle",4);
  state.duration=prefs.getLong("duration",minutes("focus",25)); state.remaining=prefs.getLong("remaining",state.duration);
  state.running=prefs.getBoolean("running",false); state.deadline=prefs.getLong("deadline",0);
 }
 public long minutes(String key,int fallback) { return prefs.getInt(key,fallback)*60_000L; }
 public String label() { return state.mode==0?"집중":state.mode==1?"짧은 휴식":"긴 휴식"; }
 public static String format(long ms) { long s=(ms+999)/1000; return String.format(Locale.US,"%02d:%02d",s/60,s%60); }
 public static PendingIntent action(Context c,String command) { return PendingIntent.getBroadcast(c,command.hashCode(),new Intent(c,TimerReceiver.class).setAction(command),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); }
 public static PendingIntent open(Context c) { return PendingIntent.getActivity(c,0,new Intent(c,MainActivity.class),PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE); }
 public void command(String command) {
  long now=SystemClock.elapsedRealtime();
  boolean ended=state.due(now);
  if(ended) {
   String endedLabel=label(); long focused=state.duration; long endedWall=System.currentTimeMillis()-Math.max(0,now-state.deadline);
   boolean credit=state.advance(true,now,minutes("focus",25),minutes("short",5),minutes("long",15),prefs.getBoolean("autoFocus",false),prefs.getBoolean("autoBreak",false));
   if(credit) record(focused,endedWall);
   notifyEnd(endedLabel);
  }
  // A stale endpoint event must never complete the next interval or double count it.
  if("toggle".equals(command) && !ended) state.toggle(now);
  if("skip".equals(command) && !ended) state.advance(false,now,minutes("focus",25),minutes("short",5),minutes("long",15),false,false);
  if("reset".equals(command)) state.reset();
  if("boot".equals(command)) { state.running=false; state.remaining=prefs.getLong("checkpoint",state.duration); }
  save(); schedule(); ongoing();
  context.sendBroadcast(new Intent(CHANGED).setPackage(context.getPackageName()));
  try { Class<?> widget=Class.forName("com.hwserve.still.StillWidget"); widget.getMethod("updateAll",Context.class).invoke(null,context); } catch(ClassNotFoundException ignored) {} catch(Exception ex) { android.util.Log.e("Still","Widget update failed",ex); }
 }
 private void save() { prefs.edit().putInt("mode",state.mode).putInt("completed",state.completed).putLong("duration",state.duration).putLong("remaining",state.remaining).putLong("deadline",state.deadline).putLong("checkpoint",state.left(SystemClock.elapsedRealtime())).putBoolean("running",state.running).apply(); }
 private void record(long duration,long wall) {
  try { JSONArray list=new JSONArray(prefs.getString("history","[]")); list.put(new JSONObject().put("date",Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate().toString()).put("minutes",duration/60_000)); prefs.edit().putString("history",list.toString()).apply(); } catch(JSONException ex) { android.util.Log.e("Still","History",ex); }
 }
 public int[] stats(String day) { int[] r={0,0}; try { JSONArray a=new JSONArray(prefs.getString("history","[]")); for(int i=0;i<a.length();i++){ JSONObject o=a.getJSONObject(i); if(day.equals(o.getString("date"))) { r[0]++;r[1]+=o.getInt("minutes"); } } } catch(JSONException ignored) {} return r; }
 private void schedule() {
  AlarmManager am=context.getSystemService(AlarmManager.class); PendingIntent end=action(context,"end"); am.cancel(end);
  if(!state.running) return;
  if(Build.VERSION.SDK_INT<31 || am.canScheduleExactAlarms()) am.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,state.deadline,end);
  else am.setAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP,state.deadline,end);
 }
 private NotificationManager manager() { return context.getSystemService(NotificationManager.class); }
 private boolean allowed() { return Build.VERSION.SDK_INT<33 || context.checkSelfPermission("android.permission.POST_NOTIFICATIONS")==android.content.pm.PackageManager.PERMISSION_GRANTED; }
 private void ongoing() {
  NotificationManager nm=manager(); nm.createNotificationChannel(new NotificationChannel("timer","진행 중인 타이머",NotificationManager.IMPORTANCE_LOW));
  if(!state.running) { nm.cancel(1); return; }
  Notification n=new Notification.Builder(context,"timer").setSmallIcon(R.drawable.ic_still).setContentTitle(label()).setContentText("집중의 흐름을 이어가세요").setContentIntent(open(context)).setOngoing(true).setOnlyAlertOnce(true).setWhen(System.currentTimeMillis()+state.left(SystemClock.elapsedRealtime())).setUsesChronometer(true).setChronometerCountDown(true).addAction(new Notification.Action.Builder(null,"일시정지",action(context,"toggle")).build()).addAction(new Notification.Action.Builder(null,"건너뛰기",action(context,"skip")).build()).build();
  if(allowed()) nm.notify(1,n);
 }
 private void notifyEnd(String ended) {
  boolean sound=prefs.getBoolean("sound",true), vibration=prefs.getBoolean("vibration",true);
  String id="end_"+sound+"_"+vibration;
  NotificationChannel ch=new NotificationChannel(id,"종료 · "+(sound?"소리":"무음")+" · "+(vibration?"진동":"진동 없음"),NotificationManager.IMPORTANCE_HIGH);
  if(!sound) ch.setSound(null,null); ch.enableVibration(vibration); ch.setVibrationPattern(new long[]{0,180,100,180}); manager().createNotificationChannel(ch);
  if(allowed()) manager().notify(2,new Notification.Builder(context,id).setSmallIcon(R.drawable.ic_still).setContentTitle(ended+" 완료").setContentText(state.running?label()+"을 시작했어요":label()+"을 시작할 시간이에요").setContentIntent(open(context)).setAutoCancel(true).addAction(new Notification.Action.Builder(null,"타이머 열기",open(context)).build()).build());
 }
}
