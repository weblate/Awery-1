package com.mrboomdev.awery.app;

import static com.mrboomdev.awery.app.AweryApp.toast;
import static com.mrboomdev.awery.util.NiceUtils.stream;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.mrboomdev.awery.sdk.util.Callbacks;

import org.jetbrains.annotations.Contract;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class AweryLifecycle {
	private static final String TAG = "AweryLifecycle";
	private static AweryApp app;
	private static Thread mainThread;
	private static Handler handler;

	protected static void init(AweryApp app) {
		AweryLifecycle.app = app;

		mainThread = Looper.getMainLooper().getThread();
		handler = new Handler(Looper.getMainLooper());
	}

	public static void restartApp() {
		var context = getAnyContext();
		var pm = context.getPackageManager();

		var intent = pm.getLaunchIntentForPackage(context.getPackageName());
		var component = Objects.requireNonNull(intent).getComponent();

		var mainIntent = Intent.makeRestartActivityTask(component);
		mainIntent.setPackage(context.getPackageName());
		context.startActivity(mainIntent);

		app = null;
		Runtime.getRuntime().exit(0);
	}

	public static void exitApp() {
		var activity = getAnyActivity(Activity.class);
		app = null;

		if(activity != null) activity.finishAffinity();
		else Runtime.getRuntime().exit(0);
	}

	/**
	 * DO NOT EVER USE DIRECTLY THIS CLASS!
	 * It was made just for the Android Framework to work properly!
	 */
	public static class CallbackFragment extends Fragment {
		private final Callbacks.Callback1<Intent> callback;

		public CallbackFragment(Callbacks.Callback1<Intent> callback) {
			this.callback = callback;
		}

		@Override
		@SuppressWarnings("deprecation")
		public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
			super.onActivityResult(requestCode, resultCode, data);
			callback.run(data);
		}
	}

	/**
	 * This method is a little bit hacky so after library update it can break.
	 * Please use with care!
	 * @param context Context from the {@link FragmentActivity}
	 * @author MrBoomDev
	 */
	@SuppressWarnings("deprecation")
	public static void startActivityForResult(Context context, Intent intent, Callbacks.Callback1<Intent> callback) {
		var activity = getActivity(context);

		if(activity instanceof FragmentActivity fragmentActivity) {
			var fragmentManager = fragmentActivity.getSupportFragmentManager();
			var fragment = new CallbackFragment(callback);

			fragmentManager.beginTransaction().add(fragment, String.valueOf(intent.hashCode())).commit();
			fragmentManager.executePendingTransactions();

			fragment.startActivityForResult(intent, 0);
		} else {
			throw new IllegalArgumentException("Activity must be an instance of FragmentActivity!");
		}
	}

	@Nullable
	public static Activity getActivity(Context context) {
		while(context instanceof ContextWrapper wrapper) {
			if(context instanceof Activity activity) {
				return activity;
			}

			context = wrapper.getBaseContext();
		}

		return null;
	}

	@Nullable
	@Contract(pure = true)
	public static <A extends Activity> A getAnyActivity(Class<A> requiredSuper) {
		try {
			var activities = getAllActivitiesRecursively(requiredSuper);
			if(activities.size() == 1) return activities.get(0).activity;

			// App should handle this behaviour properly or else crashes will occur
			if(activities.isEmpty()) return null;

			return stream(activities)
					.sorted(Collections.reverseOrder())
					.findFirst().get().activity;
		} catch(Exception e) {
			Log.e(TAG, "Failed to get any activity!", e);
			toast(getAppContext(), "So your device is not supported :(", 1);
			System.exit(0);
			return null;
		}
	}

	@NonNull
	@SuppressWarnings({"PrivateApi", "unchecked"})
	private static <A extends Activity> List<ActivityInfo<A>> getAllActivitiesRecursively(Class<A> requiredSuper) throws NoSuchFieldException, IllegalAccessException {
		var list = new ArrayList<ActivityInfo<A>>();

		var activityThread = getActivityThread();
		if(activityThread == null) return list;

		var activitiesField = activityThread.getClass().getDeclaredField("mActivities");
		activitiesField.setAccessible(true);
		var activities = activitiesField.get(activityThread);

		if(activities instanceof Map<?, ?> map) {
			for(var record : map.values()) {
				var recordClass = record.getClass();
				var activityField = recordClass.getDeclaredField("activity");
				activityField.setAccessible(true);

				var activity = (Activity) activityField.get(record);

				if(activity == null || !requiredSuper.isInstance(activity)) {
					continue;
				}

				var info = new ActivityInfo<A>();
				info.activity = (A) activity;
				list.add(info);

				var pausedField = recordClass.getDeclaredField("paused");
				pausedField.setAccessible(true);
				info.isPaused = Objects.requireNonNullElse((Boolean) pausedField.get(record), false);

				list.add(info);
			}
		}

		return list;
	}

	@Nullable
	@SuppressLint({"DiscouragedPrivateApi", "PrivateApi"})
	private static Object getActivityThread() {
		try {
			var clazz = Class.forName("android.app.ActivityThread");
			var field = clazz.getDeclaredField("sCurrentActivityThread");
			field.setAccessible(true);
			var value = field.get(null);
			if(value != null) return value;
		} catch(Exception ignored) {}

		try {
			var clazz = Class.forName("android.app.AppGlobals");
			var field = clazz.getDeclaredField("sCurrentActivityThread");
			field.setAccessible(true);
			var value = field.get(null);
			if(value != null) return value;
		} catch(Exception ignored) {}

		try {
			var clazz = Class.forName("android.app.ActivityThread");
			var method = clazz.getDeclaredMethod("currentActivityThread");
			method.setAccessible(true);
			return method.invoke(null);
		} catch(Exception ignored) {
			return null;
		}
	}

	@Nullable
	public static Runnable postRunnable(Runnable runnable) {
		return handler.post(runnable) ? runnable : null;
	}

	public static Runnable runOnUiThread(Runnable runnable) {
		if(Thread.currentThread() != mainThread) handler.post(runnable);
		else runnable.run();

		return runnable;
	}

	/**
	 * Runs the callback on the main thread and checks whatever RecyclerView is computing layout or not to avoid exceptions.
	 * @param callback Will be ran on the main thread if RecyclerView isn't computing layout
	 * @param recycler Target RecyclerView
	 * @return May be a different callback depending on the state of the RecyclerView, so that you can cancel it.
	 */
	@NonNull
	public static Runnable runOnUiThread(Runnable callback, RecyclerView recycler) {
		if(Thread.currentThread() != mainThread || recycler.isComputingLayout()) {
			Runnable runnable = () -> runOnUiThread(callback, recycler);
			handler.post(runnable);
			return runnable;
		}

		callback.run();
		return callback;
	}

	public static Context getContext(@NonNull ViewBinding binding) {
		return binding.getRoot().getContext();
	}

	public static Context getContext(@NonNull View view) {
		return view.getContext();
	}

	public static Context getContext(@NonNull LayoutInflater inflater) {
		return inflater.getContext();
	}

	public static Context getAnyContext() {
		Activity activity;

		try {
			activity = getAnyActivity(Activity.class);
			if(activity != null) return activity;
		} catch(IndexOutOfBoundsException ignored) {}

		if(app == null) {
			return getContextUsingPrivateApi();
		}

		return app;
	}

	@Nullable
	@SuppressLint({"PrivateApi","DiscouragedPrivateApi" })
	private static Context getContextUsingPrivateApi() {
		Context context = null;

		try {
			var activityThreadClass = Class.forName("android.app.ActivityThread");
			var method = activityThreadClass.getDeclaredMethod("currentApplication");
			context = (Application) method.invoke(null);
		} catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
			Log.e(TAG, "Failed to get Application from ActivityThread!", e);
		}

		if(context == null) {
			try {
				var appGlobalsClass = Class.forName("android.app.AppGlobals");
				var method = appGlobalsClass.getDeclaredMethod("getInitialApplication");
				context = (Application) method.invoke(null);
			} catch(ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
				Log.e(TAG, "Failed to get Application from AppGlobals!", e);
			}
		}

		if(context != null) {
			Log.w(TAG, "Using Context from a static method!");
		}

		return context;
	}

	public static Context getAppContext() {
		return app;
	}

	public static void cancelDelayed(Runnable runnable) {
		handler.removeCallbacks(runnable);
	}

	public static void runDelayed(Runnable runnable, long delay) {
		handler.postDelayed(runnable, delay);
	}

	private static class ActivityInfo<A extends Activity> implements Comparable<ActivityInfo<A>> {
		public A activity;
		public boolean isPaused;

		@Override
		public int compareTo(ActivityInfo o) {
			if(hasWindowFocus(activity) && !hasWindowFocus(o.activity)) return 1;
			if(!hasWindowFocus(activity) && hasWindowFocus(o.activity)) return -1;

			if(isPaused && !o.isPaused) return -1;
			if(!isPaused && o.isPaused) return 1;

			return Integer.compare(activity.getTaskId(), o.activity.getTaskId());
		}

		/**
		 * Sometimes Android do throw this exception "java.lang.RuntimeException: Window couldn't find content container view".
		 * Because we just need to check if the activity has focus or not we ignore the exception and return false.
		 * @return true if the activity has focus or false if it doesn't
		 * @author MrBoomDev
		 */
		private boolean hasWindowFocus(Activity activity) {
			try {
				return activity.hasWindowFocus();
			} catch(RuntimeException e) {
				return false;
			}
		}
	}
}