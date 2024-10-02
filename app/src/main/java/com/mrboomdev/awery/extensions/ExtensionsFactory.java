package com.mrboomdev.awery.extensions;

import static com.mrboomdev.awery.app.App.toast;
import static com.mrboomdev.awery.app.AweryLifecycle.getAppContext;
import static com.mrboomdev.awery.util.NiceUtils.stream;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringDef;

import com.mrboomdev.awery.data.Constants;
import com.mrboomdev.awery.extensions.support.internal.InternalManager;
import com.mrboomdev.awery.extensions.support.js.JsManager;
import com.mrboomdev.awery.extensions.support.yomi.YomiHelper;
import com.mrboomdev.awery.extensions.support.yomi.aniyomi.AniyomiManager;
import com.mrboomdev.awery.util.NiceUtils;
import com.mrboomdev.awery.util.Progress;
import com.mrboomdev.awery.util.async.AsyncFuture;
import com.mrboomdev.awery.util.async.AsyncUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import java9.util.Objects;
import java9.util.stream.StreamSupport;

public class ExtensionsFactory {
	private static ExtensionsFactory instance;
	private static final String TAG = "ExtensionsFactory";
	private static AsyncFuture<ExtensionsFactory> pendingFuture;
	protected static final Progress progress = new Progress();
	private final List<ExtensionsManager> managers = new ArrayList<>();

	/**
	 * This method will not try load the ExtensionsFactory, so use it only if you know why you want to.
	 * @author MrBoomDev
	 */
	@Nullable
	public static ExtensionsFactory getInstanceNow() {
		return instance;
	}

	public List<ExtensionsManager> getManagers() {
		return managers;
	}

	@NonNull
	public static AsyncFuture<ExtensionsFactory> getInstance() {
		if(instance != null) {
			return AsyncUtils.futureNow(instance);
		}

		if(pendingFuture != null) {
			return pendingFuture;
		}

		return pendingFuture = AsyncUtils.controllableFuture(future -> {
			new ExtensionsFactory(getAppContext());
			pendingFuture = null;
			future.complete(instance);
		});
	}

	private ExtensionsFactory(@NonNull Application context) {
		Log.d(TAG, "Start loading...");
		instance = this;
		YomiHelper.init(context);

		managers.add(new AniyomiManager());
		managers.add(new InternalManager());

		//managers.add(new TachiyomiManager()); // We doesn't support manga reading at the moment so we can just disable it for now

		/*List.of(
				,
				//
				//new CloudstreamManager(),
				//new MiruManager(),
				//new JsManager(), //J s extensions were a bad decision
				);*/

		for(var manager : managers) {
			manager.loadAllExtensions(context);
		}

		var failedExtensions = stream(managers)
				.map(manager -> manager.getExtensions(Extension.FLAG_ERROR))
				.flatMap(NiceUtils::stream)
				.filter(extension -> !Objects.equals(extension.getErrorTitle(), Extension.DISABLED_ERROR))
				.toList();

		if(!failedExtensions.isEmpty()) {
			Log.e(TAG, "");
			Log.e(TAG, Constants.LOGS_SEPARATOR);

			for(var extension : failedExtensions) {
				if(extension.getError() != null) Log.e(TAG, extension.getErrorTitle(), extension.getError());
				else Log.e(TAG, extension.getErrorTitle());

				Log.e(TAG, Constants.LOGS_SEPARATOR);
			}

			var text = "Failed to load " + failedExtensions.size() + " extension(s)";

			Log.e(TAG, "");
			Log.e(TAG, text);
			toast(text);
		}

		Log.d(TAG, "Finished loading");
	}

	@SuppressWarnings("unchecked")
	@Deprecated(forRemoval = true)
	public static <T extends ExtensionsManager> T getManager__Deprecated(Class<T> clazz) {
		return (T) stream(getInstance().await().managers)
				.filter(manager -> manager.getClass() == clazz)
				.findFirst().orElseThrow();
	}

	@Deprecated(forRemoval = true)
	public static ExtensionsManager getManager__Deprecated(@NonNull String name) {
		return getManager__Deprecated((Class<? extends ExtensionsManager>) switch(name) {
			case AniyomiManager.MANAGER_ID -> AniyomiManager.class;
			case JsManager.MANAGER_ID -> JsManager.class;
			case InternalManager.MANAGER_ID -> InternalManager.class;
			default -> throw new IllegalArgumentException("Extensions manager \"" + name + "\" was not found!");
		});
	}

	@NonNull
	@Deprecated(forRemoval = true)
	public static Collection<Extension> getExtensions__Deprecated(int flags) {
		return stream(getInstance().await().managers)
				.map(manager -> manager.getExtensions(flags))
				.flatMap(StreamSupport::stream).toList();
	}

	@SuppressWarnings("unchecked")
	public <T extends ExtensionsManager> T getManager(Class<T> clazz) {
		return (T) stream(managers)
				.filter(manager -> manager.getClass() == clazz)
				.findFirst().orElseThrow();
	}

	@StringDef({ "ANIYOMI_KOTLIN", "AWERY_JS", "INTERNAL" })
	public @interface ExtensionName {}

	public ExtensionsManager getManager(@NonNull @ExtensionName String name) {
		return getManager((Class<? extends ExtensionsManager>) switch(name) {
			case AniyomiManager.MANAGER_ID -> AniyomiManager.class;
			case JsManager.MANAGER_ID -> JsManager.class;
			case InternalManager.MANAGER_ID -> InternalManager.class;
			default -> throw new IllegalArgumentException("Extensions manager \"" + name + "\" was not found!");
		});
	}

	@NonNull
	public Collection<Extension> getExtensions(int flags) {
		return stream(managers)
				.map(manager -> manager.getExtensions(flags))
				.flatMap(StreamSupport::stream).toList();
	}
}