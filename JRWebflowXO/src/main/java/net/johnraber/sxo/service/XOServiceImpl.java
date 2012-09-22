package net.johnraber.sxo.service;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cache.Cache.ValueWrapper;
import org.springframework.cache.ehcache.EhCacheCache;
import org.springframework.cache.ehcache.EhCacheCacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import net.johnraber.sxo.model.XOSession;
import net.johnraber.sxo.persistence.XOSessionDao;
import net.johnraber.sxo.utility.XOSessionDataUtility;


@Service
public class XOServiceImpl implements XOService
{
	private static final Logger log = LoggerFactory.getLogger(XOServiceImpl.class);
	
	@Autowired
	@Qualifier("springEhCacheManager")
	private EhCacheCacheManager  cacheMgr;
	
//	@Autowired
//	@Qualifier("xoCache")
	private EhCacheCache   xoCache;
	
	@Autowired
	XOSessionDao xoSessionDao;
	
	// horrible hack for now
	private static int cacheInited = 1;
	
	
	public XOServiceImpl()
	{
//		xoCache = (EhCacheCache) cacheMgr.getCache("xoCache");
//		log.debug("Used cache manager to retrieve cache: 'xoCache'.");
	}
	
	@Override
	@Transactional(propagation=Propagation.REQUIRED )
	public XOSession createXOSession(String merchantID)
	{
		XOSession xoSession = new XOSession();
		xoSession.setXosessionId( createFauxXOGuid() );
		xoSession.setMerchant(merchantID);
		
//		xoCache.put(xoSession.getXosessionId(), xoSession);
		getXoCache().put(xoSession.getXosessionId(), xoSession);
		
		return xoSession;
	}
	
	
	@Transactional(propagation=Propagation.REQUIRED)
	public XOSession getXOSession(Long xoSessionID)
	{
		XOSession xoSession = null;
//		ValueWrapper cachedXOSession = xoCache.get(xoSessionID);
		ValueWrapper cachedXOSession = getXoCache().get(xoSessionID);
		
		if( cachedXOSession != null )
		{
			xoSession = (XOSession) cachedXOSession.get();
			log.info("Retrieving XO Session from cache: " + xoSession.getDisplayString()  );
		}
		
		return xoSession; 
		
	}
	
	@Transactional(propagation=Propagation.REQUIRED)
	public XOSession getArchivedXOSession(Long xoSessionID)
	{
		//TODO  retrieve dao 
		
		XOSession xoSession = new XOSession();
		xoSession.setXosessionId(xoSessionID);
		return xoSession;
	}
	
	
	
	@Transactional(propagation=Propagation.REQUIRED)
	public XOSession updateXOSession(XOSession xoSession)
	{
		ValueWrapper cachedXOSessionVW = getXoCache().get(xoSession.getXosessionId());
		
		if( cachedXOSessionVW != null )
		{
			XOSession cachedXOSession = (XOSession) cachedXOSessionVW.get();
	
			//TODO  if service version is differently from passed in version
			// then either perform a merge based on rules or server wins and
			// client ( aka UI ) needs to adjust

			getXoCache().put(xoSession.getXosessionId(), xoSession); // this is JSR107 method
			
			log.info("Updated XO Session in cache: " + xoSession.getDisplayString()  );
		}
		else
		{
			log.info("Could NOT updated XO Session in cache: " + xoSession.getDisplayString() + " as the entity does NOT already exist"  );
			return null;
		}
	   
		// this is either merged or what caller sent in
		return xoSession;
	}
	
	
	/**
	 * This method assumes caller knows the current state ( server version )
	 * of the XOSession and wants to persist it.  Providing this method so 
	 * the entire XOSession object does not have to be passed through 
	 * for performance ( speed, network bandwidth, ...
	 * no data movement or processing needed ).  Use 
	 * commitXOSession(XOSession xoSession) if you need to pass in 
	 * changes and commit the resource.
	 *  
	 */
	@Transactional(propagation=Propagation.REQUIRED)
	public XOSession commitXOSession(Long xoSessionID)
	{
		ValueWrapper cachedXOSessionVW = getXoCache().get(xoSessionID);
		
		if( cachedXOSessionVW != null )
		{
			XOSession cachedXOSession = (XOSession) cachedXOSessionVW.get();
		
			if( commitXOSession(cachedXOSession) )
			{
				return cachedXOSession;
			}
			return null;
		}
		else 
		{
			log.debug("XO Session: " + xoSessionID + " does NOT exists in cache so can NOT be committed!");
			return null;
		}
	}
	
	
	/**
	 * This version of the xoSession will be committed with no
	 * merge/biz rules run of the XO session.  LAST in WINS!
	 * @param xoSession
	 * @return
	 */
	@Transactional(propagation=Propagation.REQUIRED)
	public boolean commitXOSession(XOSession xoSession)
	{
		//TODO use canonical/service level POJO 
		// for XOSession and persist into database
		// using the domain model representation
		net.johnraber.sxo.persistence.model.XOSession domainXOSession =
				XOSessionDataUtility.createPersistentModel(xoSession);
		
		//TODO in real world ensure save is not a created/merge because
		// you want to throw if the XOSession already exists
		xoSessionDao.saveXOSession( domainXOSession );
		
		getXoCache().evict( xoSession.getXosessionId() );
		log.debug("Persisting XO Session: " + xoSession + " and removing from cache.");
		
		return true;
	}
	
	private long createFauxXOGuid()
	{
		double ohyeah = 10000;
		return (long) ( Math.random() * ohyeah );
	}
	
	// using get method because am not able to
	// init  when this class is loaded or in
	// the constructor due to some ordering/timing of
	// dependent resources in ecache init
	public EhCacheCache getXoCache()
	{
		if( cacheMgr == null )
		{
			log.error("Cache manager is null ... dead in the water ... returning null!");
			return null;
		}
		
		
		if( xoCache == null || cacheInited == 1)
		{
			org.springframework.cache.Cache bla = cacheMgr.getCache("xoCache");
			if( bla instanceof EhCacheCache)
			{
				xoCache = (EhCacheCache) cacheMgr.getCache("xoCache");
				log.debug("Had to use cache manager to retrieve cache since cache variable was null: 'xoCache'.");
			}
			else
			{
				log.error("Cache manager retrieving the wrong flavor of cache ( Spring instead of EhCache )!");
				return null;
			}
			
		}

		cacheInited++;
	
		return xoCache;
	}

}
