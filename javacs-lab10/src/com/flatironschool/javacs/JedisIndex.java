package com.flatironschool.javacs;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.jsoup.select.Elements;

import redis.clients.jedis.Jedis;
import redis.clients.jedis.Transaction;

/**
 * Represents a Redis-backed web search index.
 * 
 */
public class JedisIndex {

	private Jedis jedis;

	/**
	 * Constructor.
	 * 
	 * @param jedis
	 */
	public JedisIndex(Jedis jedis) {
		this.jedis = jedis;
	}
	
	/**
	 * Returns the Redis key for a given search term.
	 * 
	 * @return Redis key.
	 */
	private String urlSetKey(String term) {
		return "URLSet:" + term;
	}
	
	/**
	 * Returns the Redis key for a URL's TermCounter.
	 * 
	 * @return Redis key.
	 */
	private String termCounterKey(String url) {
		return "TermCounter:" + url;
	}

	/**
	 * Checks whether we have a TermCounter for a given URL.
	 * 
	 * @param url
	 * @return
	 */
	public boolean isIndexed(String url) {
		String redisKey = termCounterKey(url);
		return jedis.exists(redisKey);
	}
	
	/**
	 * Looks up a search term and returns a set of URLs.
	 * 
	 * @param term
	 * @return Set of URLs.
	 */
	public Set<String> getURLs(String term) {
//        System.out.println("get urls");
//        //return jedis.smembers(urlSetKey(term));
//        Set<String> result = new HashSet<>();
//        for(String currentURL : urlSetKeys()){
//            if(currentURL.contains(term)){
//                result.add(currentURL);
//            }
//        }
//        return result;
        Set<String> set = jedis.smembers(urlSetKey(term));
        return set;
	}

    /**
	 * Looks up a term and returns a map from URL to count.
	 * 
	 * @param term
	 * @return Map from URL to count.
	 */
	public Map<String, Integer> getCounts(String term) {
//        System.out.println("getCounts");
//        //get the term's URLs then go through to get the count
//        Set<String> urls = getURLs(term);
//        //create the Map to return
//        Map<String, Integer> counts = new HashMap<String, Integer>();
//        
//        //go through the set of urls and add the url with its count
//        for(String url:  urls){
//            counts.put(url, getCount(url, term));
//        }
//        return counts;
//        //return null;
        Map<String, Integer> map = new HashMap<String, Integer>();
        Set<String> urls = getURLs(term);
        for (String url: urls) {
            Integer count = getCount(url, term);
            map.put(url, count);
        }
        return map;
	}

    /**
	 * Returns the number of times the given term appears at the given URL.
	 * 
	 * @param url
	 * @param term
	 * @return
	 */
	public Integer getCount(String url, String term) {
//        System.out.println("getCount");
//        //.hget() return the value of the value stored at the key
//        return Integer.parseInt(jedis.hget(termCounterKey(url), term));
//        //return null;
        
        String redisKey = termCounterKey(url);
        String count = jedis.hget(redisKey, term);
        return new Integer(count);

	}


	/**
	 * Add a page to the index.
	 * 
	 * @param url         URL of the page.
	 * @param paragraphs  Collection of elements that should be indexed.
	 */
//	public void indexPage(String url, Elements paragraphs) {
////        System.out.println("index page");
////        //create a new term counter
////        TermCounter counter = new TermCounter(url);
////        counter.processElements(paragraphs);
////        
////        for(String term: counter.keySet()){
////            jedis.hset("TermCounter:"+url, term, counter.get(term).toString());
////            //add a TermCounter to the set associated with term
////           jedis.sadd("URLSet:" + term, url);
////        }
//        
//        
//        TermCounter tc = new TermCounter(url);
//        // Count all words in elements first
//        tc.processElements(paragraphs);
//        // Add URL associated with each word to the term counter
//        for (String word : tc.keySet())
//        {
//            tc.sadd(word, tc);
//            // Send information over to Jedis to be stored in a hash
//            jedis.hset(termCounterKey(url), word, Integer.toString(tc.get(word)));
//        }
//        
	//}

    public void indexPage(String url, Elements paragraphs) {
        TermCounter tc = new TermCounter(url);
        tc.processElements(paragraphs);
        this.processTermCounter(tc);
    }

/**
 * Processes TermCounter by taking its url, terms, and term counts and adding them to Redis data structures.
 *
 * @param tc
 */
    private void processTermCounter(TermCounter tc){
        String counterURL = tc.getLabel();
        Transaction t = jedis.multi();
        String termCounterKey = this.termCounterKey(counterURL);
        for(String term : tc.keySet()){
            t.sadd(this.urlSetKey(term), counterURL);
            t.hset(termCounterKey, term, tc.get(term).toString());
        }
        t.exec();
    }

	/**
	 * Prints the contents of the index.
	 * 
	 * Should be used for development and testing, not production.
	 */
	public void printIndex() {
		// loop through the search terms
		for (String term: termSet()) {
			System.out.println(term);
			
			// for each term, print the pages where it appears
			Set<String> urls = getURLs(term);
			for (String url: urls) {
				Integer count = getCount(url, term);
				System.out.println("    " + url + " " + count);
			}
		}
	}

	/**
	 * Returns the set of terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termSet() {
		Set<String> keys = urlSetKeys();
		Set<String> terms = new HashSet<String>();
		for (String key: keys) {
			String[] array = key.split(":");
			if (array.length < 2) {
				terms.add("");
			} else {
				terms.add(array[1]);
			}
		}
		return terms;
	}

	/**
	 * Returns URLSet keys for the terms that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> urlSetKeys() {
		return jedis.keys("URLSet:*");
	}

	/**
	 * Returns TermCounter keys for the URLS that have been indexed.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public Set<String> termCounterKeys() {
		return jedis.keys("TermCounter:*");
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteURLSets() {
		Set<String> keys = urlSetKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all URLSet objects from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteTermCounters() {
		Set<String> keys = termCounterKeys();
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * Deletes all keys from the database.
	 * 
	 * Should be used for development and testing, not production.
	 * 
	 * @return
	 */
	public void deleteAllKeys() {
		Set<String> keys = jedis.keys("*");
		Transaction t = jedis.multi();
		for (String key: keys) {
			t.del(key);
		}
		t.exec();
	}

	/**
	 * @param args
	 * @throws IOException 
	 */
	public static void main(String[] args) throws IOException {
		Jedis jedis = JedisMaker.make();
		JedisIndex index = new JedisIndex(jedis);
		
		//index.deleteTermCounters();
		//index.deleteURLSets();
		//index.deleteAllKeys();
		loadIndex(index);
		
		Map<String, Integer> map = index.getCounts("the");
		for (Entry<String, Integer> entry: map.entrySet()) {
			System.out.println(entry);
		}
	}

	/**
	 * Stores two pages in the index for testing purposes.
	 * 
	 * @return
	 * @throws IOException
	 */
	private static void loadIndex(JedisIndex index) throws IOException {
		WikiFetcher wf = new WikiFetcher();

		String url = "https://en.wikipedia.org/wiki/Java_(programming_language)";
		Elements paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
		
		url = "https://en.wikipedia.org/wiki/Programming_language";
		paragraphs = wf.readWikipedia(url);
		index.indexPage(url, paragraphs);
	}
}
