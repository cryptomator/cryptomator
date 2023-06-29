package org.cryptomator.common;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.Enumeration;
import java.util.InvalidPropertiesFormatException;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Pattern;

public class LazyProcessedProperties extends Properties {

	private static final Logger LOG = LoggerFactory.getLogger(LazyProcessedProperties.class);

	//Template and env _need_ to be instance variables, otherwise they might not be initialized at access time
	private final Pattern template = Pattern.compile("@\\{(\\w+)}");
	private final Map<String, String> env;
	private final Properties delegate;

	public LazyProcessedProperties(Properties props, Map<String, String> systemEnvironment) {
		this.delegate = props;
		this.env = systemEnvironment;
	}

	@Override
	public String getProperty(String key) {
		var value = delegate.getProperty(key);
		if (key.startsWith("cryptomator.") && value != null) {
			return process(value);
		} else {
			return value;
		}
	}

	@Override
	public String getProperty(String key, String defaultValue) {
		var value = delegate.getProperty(key, defaultValue);
		if (key.startsWith("cryptomator.") && value != null) {
			return process(value);
		} else {
			return value;
		}
	}

	//visible for testing
	String process(String value) {
		return template.matcher(value).replaceAll(match -> //
				switch (match.group(1)) {
					case "appdir" -> resolveFrom("APPDIR", Source.ENV);
					case "appdata" -> resolveFrom("APPDATA", Source.ENV);
					case "localappdata" -> resolveFrom("LOCALAPPDATA", Source.ENV);
					case "userhome" -> resolveFrom("user.home", Source.PROPS);
					default -> {
						LOG.warn("Unknown variable @{{}} in property value {}.", match.group(), value);
						yield match.group();
					}
				});
	}

	private String resolveFrom(String key, Source src) {
		var val = switch (src) {
			case ENV -> env.get(key);
			case PROPS -> delegate.getProperty(key);
		};
		if (val == null) {
			LOG.warn("Variable {} used for substitution not found in {}. Replaced with empty string.", key, src);
			return "";
		} else {
			return val.replace("\\", "\\\\");
		}
	}

	private enum Source {
		ENV,
		PROPS;
	}

	@Override
	public Object setProperty(String key, String value) {
		return delegate.setProperty(key, value);
	}

	//auto generated
	@Override
	public void load(Reader reader) throws IOException {delegate.load(reader);}

	@Override
	public void load(InputStream inStream) throws IOException {delegate.load(inStream);}

	@Override
	@Deprecated
	public void save(OutputStream out, String comments) {delegate.save(out, comments);}

	@Override
	public void store(Writer writer, String comments) throws IOException {delegate.store(writer, comments);}

	@Override
	public void store(OutputStream out, @Nullable String comments) throws IOException {delegate.store(out, comments);}

	@Override
	public void loadFromXML(InputStream in) throws IOException, InvalidPropertiesFormatException {delegate.loadFromXML(in);}

	@Override
	public void storeToXML(OutputStream os, String comment) throws IOException {delegate.storeToXML(os, comment);}

	@Override
	public void storeToXML(OutputStream os, String comment, String encoding) throws IOException {delegate.storeToXML(os, comment, encoding);}

	@Override
	public void storeToXML(OutputStream os, String comment, Charset charset) throws IOException {delegate.storeToXML(os, comment, charset);}

	@Override
	public Enumeration<?> propertyNames() {return delegate.propertyNames();}

	@Override
	public Set<String> stringPropertyNames() {return delegate.stringPropertyNames();}

	@Override
	public void list(PrintStream out) {delegate.list(out);}

	@Override
	public void list(PrintWriter out) {delegate.list(out);}

	@Override
	public int size() {return delegate.size();}

	@Override
	public boolean isEmpty() {return delegate.isEmpty();}

	@Override
	public Enumeration<Object> keys() {return delegate.keys();}

	@Override
	public Enumeration<Object> elements() {return delegate.elements();}

	@Override
	public boolean contains(Object value) {return delegate.contains(value);}

	@Override
	public boolean containsValue(Object value) {return delegate.containsValue(value);}

	@Override
	public boolean containsKey(Object key) {return delegate.containsKey(key);}

	@Override
	public Object get(Object key) {return delegate.get(key);}

	@Override
	public Object put(Object key, Object value) {return delegate.put(key, value);}

	@Override
	public Object remove(Object key) {return delegate.remove(key);}

	@Override
	public void putAll(Map<?, ?> t) {delegate.putAll(t);}

	@Override
	public void clear() {delegate.clear();}

	@Override
	public String toString() {return delegate.toString();}

	@Override
	public Set<Object> keySet() {return delegate.keySet();}

	@Override
	public Collection<Object> values() {return delegate.values();}

	@Override
	public Set<Map.Entry<Object, Object>> entrySet() {return delegate.entrySet();}

	@Override
	public boolean equals(Object o) {return delegate.equals(o);}

	@Override
	public int hashCode() {return delegate.hashCode();}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {return delegate.getOrDefault(key, defaultValue);}

	@Override
	public void forEach(BiConsumer<? super Object, ? super Object> action) {delegate.forEach(action);}

	@Override
	public void replaceAll(BiFunction<? super Object, ? super Object, ?> function) {delegate.replaceAll(function);}

	@Override
	public Object putIfAbsent(Object key, Object value) {return delegate.putIfAbsent(key, value);}

	@Override
	public boolean remove(Object key, Object value) {return delegate.remove(key, value);}

	@Override
	public boolean replace(Object key, Object oldValue, Object newValue) {return delegate.replace(key, oldValue, newValue);}

	@Override
	public Object replace(Object key, Object value) {return delegate.replace(key, value);}

	@Override
	public Object computeIfAbsent(Object key, Function<? super Object, ?> mappingFunction) {return delegate.computeIfAbsent(key, mappingFunction);}

	@Override
	public Object computeIfPresent(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {return delegate.computeIfPresent(key, remappingFunction);}

	@Override
	public Object compute(Object key, BiFunction<? super Object, ? super Object, ?> remappingFunction) {return delegate.compute(key, remappingFunction);}

	@Override
	public Object merge(Object key, Object value, BiFunction<? super Object, ? super Object, ?> remappingFunction) {return delegate.merge(key, value, remappingFunction);}

	@Override
	public Object clone() {return delegate.clone();}

	public static <K, V> Map<K, V> of() {return Map.of();}

	public static <K, V> Map<K, V> of(K k1, V v1) {return Map.of(k1, v1);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2) {return Map.of(k1, v1, k2, v2);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3) {return Map.of(k1, v1, k2, v2, k3, v3);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9);}

	public static <K, V> Map<K, V> of(K k1, V v1, K k2, V v2, K k3, V v3, K k4, V v4, K k5, V v5, K k6, V v6, K k7, V v7, K k8, V v8, K k9, V v9, K k10, V v10) {return Map.of(k1, v1, k2, v2, k3, v3, k4, v4, k5, v5, k6, v6, k7, v7, k8, v8, k9, v9, k10, v10);}

	@SafeVarargs
	public static <K, V> Map<K, V> ofEntries(Map.Entry<? extends K, ? extends V>... entries) {return Map.ofEntries(entries);}

	public static <K, V> Map.Entry<K, V> entry(K k, V v) {return Map.entry(k, v);}

	public static <K, V> Map<K, V> copyOf(Map<? extends K, ? extends V> map) {return Map.copyOf(map);}
}
