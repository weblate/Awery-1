package com.mrboomdev.awery.util;

import androidx.annotation.Nullable;

import java.util.List;

public interface ObservableList<E> extends List<E> {

	@Override
	default boolean add(E e) {
		return add(e, true);
	}

	@Override
	default void add(int index, E element) {
		add(index, element, true);
	}

	@Override
	default E remove(int index) {
		return remove(index, true);
	}

	@Override
	default boolean remove(@Nullable Object o) {
		return remove(o, true);
	}

	boolean remove(Object o, boolean callObservers);

	E remove(int index, boolean callObservers);

	void add(int index, E element, boolean callObservers);

	boolean add(E e, boolean callObservers);

	@Override
	default E set(int index, E element) {
		return set(index, element, true);
	}

	E set(int index, E element, boolean callObservers);

	void observeAdditions(AddObserver<E> callback);

	void observeChanges(ChangeObserver<E> callback);

	void observeRemovals(RemoveObserver<E> callback);

	void removeObserver(AddObserver<E> observer);

	void removeObserver(RemoveObserver<E> observer);

	void removeObserver(ChangeObserver<E> observer);

	interface RemoveObserver<E> {
		void removed(E item, int index);
	}

	interface ChangeObserver<E> {
		void changed(E newItem, E previousItem, int index);
	}

	interface AddObserver<E> {
		void added(E item, int index);
	}
}