package org.igor.onlinegames.common;

import org.igor.onlinegames.exceptions.OnlinegamesException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class OnlinegamesUtils {
    public static <T> T getSingleValueOrNull(List<T> values) {
        if (values.size() > 1) {
            throw new OnlinegamesException("values.size() > 1");
        } else if (values.isEmpty()) {
            return null;
        } else {
            return values.get(0);
        }
    }

    public static <A> A nullSafeGetterWithDefault(A obj, A defaultValue) {
        return obj != null ? obj : defaultValue;
    }

    public static <A,B> B nullSafeGetterWithDefault(A obj, Function<A,B> getter, B defaultValue) {
        B result = nullSafeGetter(obj, getter);
        return nullSafeGetterWithDefault(result, defaultValue);
    }

    public static <A,B,C> C nullSafeGetterWithDefault(A obj,
                                                      Function<A,B> getter1,
                                                      Function<B,C> getter2,
                                                      C defaultValue) {
        C result = nullSafeGetter(obj, getter1, getter2);
        return nullSafeGetterWithDefault(result, defaultValue);
    }

    public static <A,B,C,D> D nullSafeGetterWithDefault(A obj,
                                                        Function<A,B> getter1,
                                                        Function<B,C> getter2,
                                                        Function<C,D> getter3,
                                                        D defaultValue) {
        D result = nullSafeGetter(obj, getter1, getter2, getter3);
        return nullSafeGetterWithDefault(result, defaultValue);
    }

    public static <A,B,C,D,E> E nullSafeGetterWithDefault(A obj,
                                                        Function<A,B> getter1,
                                                        Function<B,C> getter2,
                                                        Function<C,D> getter3,
                                                        Function<D,E> getter4,
                                                        E defaultValue) {
        E result = nullSafeGetter(obj, getter1, getter2, getter3, getter4);
        return nullSafeGetterWithDefault(result, defaultValue);
    }

    public static <A,B,C,D,E,F> F nullSafeGetterWithDefault(A obj,
                                                        Function<A,B> getter1,
                                                        Function<B,C> getter2,
                                                        Function<C,D> getter3,
                                                        Function<D,E> getter4,
                                                        Function<E,F> getter5,
                                                        F defaultValue) {
        F result = nullSafeGetter(obj, getter1, getter2, getter3, getter4, getter5);
        return nullSafeGetterWithDefault(result, defaultValue);
    }

    public static <A,B> B nullSafeGetter(A obj, Function<A,B> getter) {
        return obj == null ? null : getter.apply(obj);
    }

    public static <A,B,C> C nullSafeGetter(A obj, Function<A,B> getter1, Function<B,C> getter2) {
        return nullSafeGetter(
                obj,
                a -> nullSafeGetter(
                        getter1.apply(a),
                        b -> getter2.apply(b)
                )
        );
    }

    public static <A,B,C,D> D nullSafeGetter(A obj, Function<A,B> getter1, Function<B,C> getter2, Function<C,D> getter3) {
        return nullSafeGetter(
                obj,
                a -> getter1.apply(a),
                b -> nullSafeGetter(
                        getter2.apply(b),
                        c -> getter3.apply(c)
                )
        );
    }

    public static <A,B,C,D,E> E nullSafeGetter(A obj,
                                               Function<A,B> getter1,
                                               Function<B,C> getter2,
                                               Function<C,D> getter3,
                                               Function<D,E> getter4) {
        return nullSafeGetter(
                obj,
                a -> getter1.apply(a),
                b -> nullSafeGetter(
                        getter2.apply(b),
                        c -> getter3.apply(c),
                        d -> getter4.apply(d)
                )
        );
    }

    public static <A,B,C,D,E,F> F nullSafeGetter(A obj,
                                               Function<A,B> getter1,
                                               Function<B,C> getter2,
                                               Function<C,D> getter3,
                                               Function<D,E> getter4,
                                               Function<E,F> getter5) {
        return nullSafeGetter(
                obj,
                a -> getter1.apply(a),
                b -> nullSafeGetter(
                        getter2.apply(b),
                        c -> getter3.apply(c),
                        d -> getter4.apply(d),
                        e -> getter5.apply(e)
                )
        );
    }

    public static <A,B> Set<B> map(Set<A> set, Function<A,B> mapper) {
        Set<B> result = new HashSet<>();
        for (A a : set) {
            B b = mapper.apply(a);
            result.add(b);
        }
        return result;
    }

    public static <A,B> List<B> map(List<A> list, Function<A,B> mapper) {
        List<B> result = new ArrayList<>();
        for (A a : list) {
            B b = mapper.apply(a);
            result.add(b);
        }
        return result;
    }

    public static <A,B> List<B> map(A[] array, Function<A,B> mapper) {
        List<B> res = new ArrayList<>();
        for (A a : array) {
            res.add(mapper.apply(a));
        }
        return res;
    }

    public static <A,B> Set<B> mapToSet(A[] array, Function<A,B> mapper) {
        Set<B> set = new HashSet<>();
        for (A a : array) {
            B b = mapper.apply(a);
            set.add(b);
        }
        return set;
    }

    public static <A,B> Set<B> mapToSet(List<A> list, Function<A,B> mapper) {
        Set<B> set = new HashSet<>();
        for (A a : list) {
            B b = mapper.apply(a);
            set.add(b);
        }
        return set;
    }

    public static <A,B> List<B> mapToList(Set<A> set, Function<A,B> mapper) {
        List<B> list = new ArrayList<>();
        for (A a : set) {
            B b = mapper.apply(a);
            list.add(b);
        }
        return list;
    }

    public static <E,K,V> Map<K,List<V>> mapToMap(Collection<E> collection,
                                                  Function<E,K> keyExtractor, Function<E,V> valueExtractor) {
        Map<K,List<V>> result = new HashMap<>();
        for (E elem : collection) {
            K key = keyExtractor.apply(elem);
            if (!result.containsKey(key)) {
                result.put(key, new ArrayList<>());
            }
            result.get(key).add(valueExtractor.apply(elem));
        }
        return result;
    }

    public static <T> Set<T> filter(Set<T> set, Predicate<T> predicate) {
        Set<T> result = new HashSet<>();
        for (T t : result) {
            if (predicate.test(t)) {
                result.add(t);
            }
        }
        return result;
    }

    public static <T> List<T> filter(List<T> list, Predicate<T> predicate) {
        List<T> result = new ArrayList<>();
        for (T elem : list) {
            if (predicate.test(elem)) {
                result.add(elem);
            }
        }
        return result;
    }

    public static <T> Set<T> filterToSet(List<T> collection, Predicate<T> predicate) {
        return collection.stream().filter(predicate).collect(Collectors.toSet());
    }

    public static <E,K,V> Map<K,V> toMap(Collection<E> collection, Function<E,K> keyExtr, Function<E,V> valueExtr) {
        return collection.stream().collect(Collectors.toMap(keyExtr, valueExtr));
    }

    public static <E,K> Map<K,E> toMap(Collection<E> collection, Function<E,K> keyExtr) {
        return toMap(collection, keyExtr, Function.identity());
    }

    public static <E> List<E> listOf(E... elems) {
        return Arrays.asList(elems);
    }

    public static <E> Set<E> setOf(E... elems) {
        Set<E> set = new HashSet<>();
        for (E elem : elems) {
            set.add(elem);
        }
        return set;
    }

    public static <K,V> Map<K,V> mapOf(K k1, V v1) {
        Map<K, V> resp = new HashMap<>();
        resp.put(k1, v1);
        return resp;
    }

    public static <K,V> Map<K,V> mapOf(K k1, V v1, K k2, V v2) {
        Map<K, V> resp = mapOf(k2,v2);
        resp.put(k1, v1);
        return resp;
    }

    public static <K,V> Map<K,V> mapOf(K k1, V v1, K k2, V v2, K k3, V v3) {
        Map<K, V> resp = mapOf(k2,v2,k3,v3);
        resp.put(k1, v1);
        return resp;
    }

    public static String replace(String content, Pattern pattern, Function<Matcher, String> replacement) {
        Matcher matcher = pattern.matcher(content);
        StringBuilder newContent = new StringBuilder();
        int prevEnd = 0;
        while (matcher.find()) {
            newContent.append(content, prevEnd, matcher.start());
            final String replacementValue = replacement.apply(matcher);
            if (replacementValue != null) {
                newContent.append(replacementValue);
            } else {
                newContent.append(matcher.group(0));
            }
            prevEnd = matcher.end();
        }
        newContent.append(content, prevEnd, content.length());
        return newContent.toString();
    }
}
