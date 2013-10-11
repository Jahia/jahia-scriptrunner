import net.sf.ehcache.Cache
import net.sf.ehcache.CacheManager
import net.sf.ehcache.Element
import org.slf4j.Logger
import org.slf4j.LoggerFactory

Logger logger = LoggerFactory.getLogger("ehCacheTool.groovy");

// a quick hack to make sure we load the serialized caches from the proper temp directory.
String oldTempDir = System.getProperty("java.io.tmpdir");
System.setProperty("java.io.tmpdir", scriptRunnerConfiguration.getBaseDirectory() + "/../../temp");
CacheManager cacheManager = CacheManager.create(scriptRunnerConfiguration.getBaseDirectory() + "/WEB-INF/classes/ehcache-jahia.xml");
System.setProperty("java.io.tmpdir", oldTempDir);

String[] cacheNames = cacheManager.getCacheNames();

for (String cacheName : cacheNames) {
    logger.info("Accessing cache " + cacheName + "...");
    Cache cache = cacheManager.getCache(cacheName);
    logger.info("  size=" + cache.getSize());
    logger.info("  loading all keys...");
    long largestSerializedSize = 0;
    Element largestSizeElement = null;
    for (Object key : cache.getKeys()) {
        Element element = cache.get(key);
        if (element != null) {
            long serializedSize = element.getSerializedSize();
            if (serializedSize > largestSerializedSize) {
                largestSerializedSize = serializedSize;
                largestSizeElement = element;
            }
        } else {
            logger.warn("    Element for key " + key.toString() + " has expired.");
        }
    }
    if (largestSizeElement != null) {
        logger.info("  largest element key=" + largestSizeElement.getKey().toString() + " serialized size=" + largestSizeElement.getSerializedSize());
    }
}

cacheManager.shutdown();