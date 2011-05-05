package uk.ac.ebi.gxa.utils;

import uk.ac.ebi.gxa.exceptions.LogUtil;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.Callable;

public class LazyList<E> implements List<E> {
    private final Callable<List<E>> factory;
    private List<E> delegate;

    public LazyList(Callable<List<E>> factory) {
        this.factory = factory;
    }

    private List<E> list() {
        try {
            if (delegate == null)
                this.delegate = factory.call();
            return delegate;
        } catch (Exception e) {
            throw LogUtil.createUnexpected("Lazy instantiation error: " + e.getMessage(), e);
        }
    }

    @Override
    public int size() {
        return list().size();
    }

    @Override
    public boolean isEmpty() {
        return list().isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return list().contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return list().iterator();
    }

    @Override
    public Object[] toArray() {
        return list().toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return list().toArray(a);
    }

    public boolean add(E e) {
        return list().add(e);
    }

    @Override
    public boolean remove(Object o) {
        return list().remove(o);
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return list().containsAll(c);
    }

    public boolean addAll(Collection<? extends E> c) {
        return list().addAll(c);
    }

    public boolean addAll(int index, Collection<? extends E> c) {
        return list().addAll(index, c);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        return list().removeAll(c);
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        return list().retainAll(c);
    }

    @Override
    public void clear() {
        list().clear();
    }

    @Override
    public boolean equals(Object o) {
        return list().equals(o);
    }

    @Override
    public int hashCode() {
        return list().hashCode();
    }

    @Override
    public E get(int index) {
        return list().get(index);
    }

    public E set(int index, E element) {
        return list().set(index, element);
    }

    public void add(int index, E element) {
        list().add(index, element);
    }

    @Override
    public E remove(int index) {
        return list().remove(index);
    }

    @Override
    public int indexOf(Object o) {
        return list().indexOf(o);
    }

    @Override
    public int lastIndexOf(Object o) {
        return list().lastIndexOf(o);
    }

    @Override
    public ListIterator<E> listIterator() {
        return list().listIterator();
    }

    @Override
    public ListIterator<E> listIterator(int index) {
        return list().listIterator(index);
    }

    @Override
    public List<E> subList(int fromIndex, int toIndex) {
        return list().subList(fromIndex, toIndex);
    }
}
