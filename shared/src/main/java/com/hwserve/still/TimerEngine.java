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
  long elapsed=SystemClock.elapsedRealtime();
  String history=null;
  if ("boot".equals(command)) history=recoverFromBoot(elapsed);
  else if ("reset".equals(command)) state.reset();
  else {
   boolean ended=state.due(elapsed);
   if (TimerState.completesOnArrival(command, ended)) history=finishInterval(elapsed);
   // A stale endpoint event must never complete the next interval or double count it.
   if ("toggle".equals(command) && !ended) state.toggle(elapsed);
   if ("skip".equals(command) && !ended) state.advance(false, elapsed, minutes("focus",25), minutes("short",5), minutes("long",15), false, false);
  }
  save(history); schedule(); ongoing();
  context.sendBroadcast(new Intent(CHANGED).setPackage(context.getPackageName()));
  try { Class<?> widget=Class.forName("com.hwserve.still.StillWidget"); widget.getMethod("updateAll",Context.class).invoke(null,context); } catch(ClassNotFoundException ignored) {} catch(Exception ex) { android.util.Log.e("Still","Widget update failed",ex); }
 }
 private String finishInterval(long elapsed) {
  String endedLabel=label(); long focused=state.duration;
  long endedWall=System.currentTimeMillis()-Math.max(0, elapsed-state.deadline);
  boolean credit=state.advance(true, elapsed, minutes("focus",25), minutes("short",5), minutes("long",15), prefs.getBoolean("autoFocus",false), prefs.getBoolean("autoBreak",false));
  notifyEnd(endedLabel);
  return credit ? historyWith(focused, endedWall) : null;
 }
 private String recoverFromBoot(long elapsed) {
  long wallNow=System.currentTimeMillis();
  long wallDeadline=prefs.getLong("wallDeadline", 0);
  TimerState.BootRecovery recovery=TimerState.BootRecovery.decide(wallNow, wallDeadline, state.duration, prefs.getLong("checkpoint", state.duration));
  state.running=false;
  if (!recovery.complete) { state.remaining=recovery.remaining; return null; }
  String endedLabel=label(); long focused=state.duration;
  boolean credit=state.advance(true, elapsed, minutes("focus",25), minutes("short",5), minutes("long",15), false, false);
  notifyEnd(endedLabel);
  state.running=false;
  return credit ? historyWith(focused, wallDeadline) : null;
 }
 private void save(String history) {
  long left=state.left(SystemClock.elapsedRealtime());
  android.content.SharedPreferences.Editor editor=prefs.edit()
   .putInt("mode",state.mode).putInt("completed",state.completed)
   .putLong("duration",state.duration).putLong("remaining",state.remaining).putLong("deadline",state.deadline)
   .putLong("checkpoint",left).putLong("wallDeadline", state.running ? System.currentTimeMillis()+left : 0)
   .putBoolean("running",state.running);
  if (history!=null) editor.putString("history", history);
  editor.apply();
 }
 private String historyWith(long duration, long wall) {
  try {
   JSONArray list=new JSONArray(prefs.getString("history","[]"));
   list.put(new JSONObject().put("date", Instant.ofEpochMilli(wall).atZone(ZoneId.systemDefault()).toLocalDate().toString()).put("minutes", duration/60_000));
   return list.toString();
  } catch(JSONException ex) { android.util.Log.e("Still","History",ex); return null; }
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
