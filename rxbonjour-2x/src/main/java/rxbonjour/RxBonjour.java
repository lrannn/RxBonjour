package rxbonjour;

import android.content.Context;
import android.support.annotation.NonNull;

import io.reactivex.Observable;
import io.reactivex.ObservableEmitter;
import io.reactivex.ObservableOnSubscribe;
import io.reactivex.android.MainThreadDisposable;
import rxbonjour.broadcast.BonjourBroadcast;
import rxbonjour.discovery.BonjourDiscovery;
import rxbonjour.exc.TypeMalformedException;
import rxbonjour.model.BonjourEvent;
import rxbonjour.model.BonjourListener;

public final class RxBonjour extends RxBonjourBase {

	private RxBonjour() {
		throw new AssertionError("no instances");
	}

	/**
	 * Starts a Bonjour service discovery for the provided service type.
	 * <p/>
	 * This method utilizes the support implementation with JmDNS as its backbone,
	 * seeing as the official NsdManager APIs are subject to multiple deal-breaking bugs. If you really want to use NsdManager on devices that
	 * support it (API level 16 or greater), use {@link #newDiscovery(Context, String, boolean)} and pass in <b>true</b> as the final argument.
	 * <p/>
	 * A {@link TypeMalformedException} is emitted after subscribing if the input type does not obey Bonjour type specifications. If you intend
	 * to use this method with arbitrary types that can be provided by user input, it is highly encouraged to verify this input
	 * using {@link #isBonjourType(String)} <b>before</b> calling this method!
	 *
	 * @param context Context of the request
	 * @param type    Type of service to discover
	 * @return An Observable for Bonjour events
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=70778">"NsdManager blocks calling thread forever" - Issue on Google Code</a>
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=35585">"Problems with Network Services Discovery APIs" - Issue on Google Code</a>
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=39750">"NSD causes Nexus 7 device to spontaneously restart." - Issue on Google Code</a>
	 */
	public static Observable<BonjourEvent> newDiscovery(Context context, String type) {
		return newDiscovery(context, type, false);
	}

	/**
	 * Starts a Bonjour service discovery for the provided service type.
	 * <p/>
	 * This method chooses the correct NSD implementation based on the device's API level. Please note that the implementation used on Jelly Bean
	 * and up is subject to multiple deal-breaking bugs, so whenever possible, the support implementation using JmDNS should be used until Google
	 * resolves these known issues with NsdManager.
	 * <p/>
	 * A {@link TypeMalformedException} is emitted after subscribing if the input type does not obey Bonjour type specifications. If you intend
	 * to use this method with arbitrary types that can be provided by user input, it is highly encouraged to verify this input
	 * using {@link #isBonjourType(String)} <b>before</b> calling this method!
	 *
	 * @param context Context of the request
	 * @param type    Type of service to discover
	 * @return An Observable for Bonjour events
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=70778">"NsdManager blocks calling thread forever" - Issue on Google Code</a>
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=35585">"Problems with Network Services Discovery APIs" - Issue on Google Code</a>
	 * @see <a href="https://code.google.com/p/android/issues/detail?id=39750">"NSD causes Nexus 7 device to spontaneously restart." - Issue on Google Code</a>
	 */
	public static Observable<BonjourEvent> newDiscovery(final Context context, final String type, final boolean forceNsdManager) {
		return Observable.create(new ObservableOnSubscribe<BonjourEvent>() {
			@Override public void subscribe(final ObservableEmitter<BonjourEvent> emitter) throws Exception {
				// Choose discovery strategy and wrap to Observable
				final BonjourDiscovery<?> discovery = BonjourDiscovery.get(forceNsdManager);
				final BonjourListener listener = new BonjourListener() {
					@Override public void onBonjourEvent(@NonNull BonjourEvent event) {
						emitter.onNext(event);
					}

					@Override public void onBonjourError(@NonNull Throwable t) {
						emitter.onError(t);
					}
				};

				// Stop listening for events when unsubscribing
				emitter.setDisposable(new MainThreadDisposable() {
					@Override protected void onDispose() {
						discovery.stop();
					}
				});

				// Start listening for events
				discovery.start(context, type, listener);
			}
		});
	}

	/**
	 * Factory to receive a Builder to create a Bonjour broadcast for the provided service type.
	 * <p/>
	 * This method chooses the correct NSD implementation based on the device's API level. Please note that the implementation used on Jelly Bean
	 * and up is subject to multiple deal-breaking bugs, so whenever possible, the support implementation using JmDNS should be used until Google
	 * resolves these known issues with NsdManager.
	 * <p/>
	 *
	 * @param context Context of the request
	 *                @param broadcast Broadcast configuration
	 * @return An Observable for Bonjour events
	 */
	public static Observable<BonjourEvent> newBroadcast(final Context context, final BonjourBroadcast<?> broadcast) {
		// Choose broadcast strategy and return its Builder object
		return Observable.create(new ObservableOnSubscribe<BonjourEvent>() {
			@Override public void subscribe(final ObservableEmitter<BonjourEvent> emitter) throws Exception {
				final BonjourListener listener = new BonjourListener() {
					@Override public void onBonjourEvent(@NonNull BonjourEvent event) {
						emitter.onNext(event);
					}

					@Override public void onBonjourError(@NonNull Throwable t) {
						emitter.onError(t);
					}
				};

				// Stop broadcasting events when unsubscribing
				emitter.setDisposable(new MainThreadDisposable() {
					@Override protected void onDispose() {
						broadcast.stop();
					}
				});

				// Start broadcasting events
				broadcast.start(context, listener);
			}
		});
	}
}