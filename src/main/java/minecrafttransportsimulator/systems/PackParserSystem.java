package minecrafttransportsimulator.systems;

import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import minecrafttransportsimulator.MasterLoader;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemPart;
import minecrafttransportsimulator.items.components.AItemPart.AItemPartCreator;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.items.instances.ItemBullet;
import minecrafttransportsimulator.items.instances.ItemDecor;
import minecrafttransportsimulator.items.instances.ItemInstrument;
import minecrafttransportsimulator.items.instances.ItemItem;
import minecrafttransportsimulator.items.instances.ItemPoleComponent;
import minecrafttransportsimulator.items.instances.ItemRoadComponent;
import minecrafttransportsimulator.items.instances.ItemVehicle;
import minecrafttransportsimulator.jsondefs.AJSONItem;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONBullet;
import minecrafttransportsimulator.jsondefs.JSONConfig;
import minecrafttransportsimulator.jsondefs.JSONDecor;
import minecrafttransportsimulator.jsondefs.JSONInstrument;
import minecrafttransportsimulator.jsondefs.JSONItem;
import minecrafttransportsimulator.jsondefs.JSONPack;
import minecrafttransportsimulator.jsondefs.JSONPart;
import minecrafttransportsimulator.jsondefs.JSONPoleComponent;
import minecrafttransportsimulator.jsondefs.JSONRoadComponent;
import minecrafttransportsimulator.jsondefs.JSONSkin;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONVehicle;
import minecrafttransportsimulator.mcinterface.InterfaceCore;
import minecrafttransportsimulator.packloading.JSONParser;
import minecrafttransportsimulator.packloading.LegacyCompatSystem;
import minecrafttransportsimulator.packloading.PackResourceLoader.ItemClassification;
import minecrafttransportsimulator.packloading.PackResourceLoader.PackStructure;

/**
 * Class responsible for parsing content pack data.  Gets properties from the text files that other parts
 * of the code can use.
 *
 * @author don_bruce
 */
public final class PackParserSystem{
	/**Links packs to the jar files that they are a part of.  Used for pack loading only: asset loading uses Java classpath systems.**/
	private static final Map<String, File> packJarMap = new HashMap<String, File>();
	
	/**All registered pack definitions are stored in this list as they are added.  Used to handle loading operations.**/
	private static final Map<String, JSONPack> packMap = new HashMap<String, JSONPack>();
	
	/**Part creators are put here during the boot process prior to parsing.  This allows for creators to be added after first parsing for custom items.**/
	private static final List<AItemPartCreator> partCreators = new ArrayList<AItemPartCreator>();

	/**All registered skin definitions are stored in this list as they are added.  These have to be added after all packs are loaded.**/
	private static final Map<String, Map<String, JSONSkin>> skinMap = new HashMap<String, Map<String, JSONSkin>>();
	
	/**List of pack faults.  This is for packs that didn't get loaded due to missing dependencies.**/
	public static final Map<String, List<String>> faultMap = new HashMap<String, List<String>>();
	
	/**All registered pack items are stored in this map as they are added.  Used to sort items in the creative tab,
	 * and will be sent to packs for item registration when so asked via {@link #getItemsForPack(String)}.  May also
	 * be used if we need to lookup a registered part item.  Map is keyed by packID to allow sorting for items from 
	 * different packs, while the sub-map is keyed by the part's {@link AJSONItem#systemName}.  The pack-map is a tree
	 * map to keep consistent tab and sorting order as packs can load at different times.  The actual items are in a 
	 * hash map and remain un-sorted.  A sorted list may be obtained by {@link #getAllItemsForPack(String, boolean)},
	 * however this is an expensive operation as the sorted list is created each call.**/
	private static final TreeMap<String, HashMap<String, AItemPack<?>>> packItemMap = new TreeMap<String, HashMap<String, AItemPack<?>>>();
	
	/**Comparator used for sorting pack items.**/
	private static final Comparator<AItemPack<?>> packComparator = new Comparator<AItemPack<?>>(){
    	@Override
		public int compare(AItemPack<?> itemA, AItemPack<?> itemB){
			String totalAName = itemA.definition.classification.toDirectory() + itemA.definition.prefixFolders + itemA.definition.systemName;
			if(itemA instanceof AItemSubTyped){
				totalAName += ((AItemSubTyped<?>) itemA).subName;
			}
			String totalBName = itemB.definition.classification.toDirectory() + itemB.definition.prefixFolders + itemB.definition.systemName;
			if(itemB instanceof AItemSubTyped){
				totalBName += ((AItemSubTyped<?>) itemB).subName;
			}
			
			if(itemA.getClass().equals(itemB.getClass()) || (itemA instanceof AItemPart && itemB instanceof AItemPart)){
				return totalAName.compareTo(totalBName);
			}else{
				return Integer.compare(getItemPriority(itemA), getItemPriority(itemB));
			}
		}
    	
    	private int getItemPriority(AItemPack<?> item){
    		if(item instanceof ItemVehicle) return 0;
    		if(item instanceof AItemPart) return 1;
    		if(item instanceof ItemBullet) return 2;
    		if(item instanceof ItemDecor) return 3;
    		if(item instanceof ItemPoleComponent) return 4;
    		if(item instanceof ItemRoadComponent) return 5;
    		if(item instanceof ItemItem) return 6;
    		if(item instanceof ItemInstrument) return 7;
    		return Integer.MAX_VALUE;
    	}
    };
	

    //-----START OF NEW INIT LOGIC-----
	/**
     * Called to parse all packs and set up the main mod.  All directories in the passed-in list will be checked
     * for pack definitions.  After this, they will be created and loaded into the main mod.
     */
    public static void parsePacks(List<File> packDirectories){
    	//First get all pack definitions from the passed-in directories.
    	for(File directory : packDirectories){
    		for(File file : directory.listFiles()){
    			if(file.getName().endsWith(".jar")){
					checkJarForPacks(file);
				}
    		}
    	}
    	
    	//Next, parse all packs in those definitions.
    	parseAllPacks();
    	
    	//Check for custom skins.
    	parseAllSkins();
    	
    	//Check to make sure we have all our fuels.  We may have loaded a new engine type this launch.
    	if(ConfigSystem.configObject.fuel.fuels == null){
    		ConfigSystem.configObject.fuel.fuels = new HashMap<String, Map<String, Double>>();
    	}
		for(Entry<String, Map<String, Double>> fuelValues : JSONConfig.ConfigFuel.getDefaultFuels().entrySet()){
			if(!ConfigSystem.configObject.fuel.fuels.containsKey(fuelValues.getKey())){
				ConfigSystem.configObject.fuel.fuels.put(fuelValues.getKey(), fuelValues.getValue());
				ConfigSystem.saveToDisk();
				break;
			}
		}
    	
    	//Have the config system dump the crafting, if so required.
    	ConfigSystem.initCraftingOverrides();
    }
    
    /**
     * This should only be called once.  This adds the default internal items
     * into the mod.  These are hard-coded to the main mod itself.  Normally,
     * we would just let the parser get these items.  But we can't do this as
     * when in a decompiled dev environment the items are in folders, not a jar. 
     */
    public static void addDefaultItems(){
		try{
			JSONPack packDef = new JSONPack();
			packDef.packID = MasterLoader.MODID;
			packDef.fileStructure = 0;
			packDef.packName = InterfaceCore.getModName(MasterLoader.MODID);
			packDef.packItem = "wrench";
			PackParserSystem.packMap.put(MasterLoader.MODID, packDef);
			
			Map<String, ItemClassification> defaultItems = new HashMap<String, ItemClassification>();
			defaultItems.put("fuelhose", ItemClassification.ITEM);
			defaultItems.put("handbook_car", ItemClassification.ITEM);
			defaultItems.put("handbook_plane", ItemClassification.ITEM);
			defaultItems.put("jumpercable", ItemClassification.ITEM);
			defaultItems.put("jumperpack", ItemClassification.ITEM);
			defaultItems.put("key", ItemClassification.ITEM);
			defaultItems.put("jumperpack", ItemClassification.ITEM);
			defaultItems.put("paintgun", ItemClassification.ITEM);
			defaultItems.put("partscanner", ItemClassification.ITEM);
			defaultItems.put("ticket", ItemClassification.ITEM);
			defaultItems.put("wrench", ItemClassification.ITEM);
			defaultItems.put("y2kbutton", ItemClassification.ITEM);
			defaultItems.put("jerrycan", ItemClassification.PART);
			defaultItems.put("fuelpump", ItemClassification.DECOR);
			defaultItems.put("vehiclebench", ItemClassification.DECOR);
			defaultItems.put("enginebench", ItemClassification.DECOR);
			defaultItems.put("propellerbench", ItemClassification.DECOR);
			defaultItems.put("wheelbench", ItemClassification.DECOR);
			defaultItems.put("seatbench", ItemClassification.DECOR);
			defaultItems.put("gunbench", ItemClassification.DECOR);
			defaultItems.put("custombench", ItemClassification.DECOR);
			defaultItems.put("instrumentbench", ItemClassification.DECOR);
			defaultItems.put("decorbench", ItemClassification.DECOR);
			defaultItems.put("itembench", ItemClassification.DECOR);
			
			String prefixFolders = "/assets/" + MasterLoader.MODID + "/jsondefs/";
			for(Entry<String, ItemClassification> defaultItem : defaultItems.entrySet()){
				String systemName = defaultItem.getKey();
				ItemClassification classification = defaultItem.getValue();
				InputStreamReader reader = new InputStreamReader(MasterLoader.class.getResourceAsStream(prefixFolders + classification.toDirectory() + systemName + ".json"), "UTF-8");
				AJSONItem itemDef = JSONParser.parseStream(reader, classification.representingClass, packDef.packID, systemName);
				itemDef.packID = packDef.packID;
				itemDef.systemName = systemName;
				itemDef.classification = classification;
				itemDef.prefixFolders = prefixFolders;
				PackParserSystem.registerItem(itemDef);
			}
		}catch(Exception e){
			e.printStackTrace();
		}
    }
    
    /**
     * Called to add a part item creator to the main listing. 
     */
    public static void addItemPartCreator(AItemPartCreator creator){
    	partCreators.add(0, creator);
    }

	/**
     * Called to check if the passed-in jar file is a pack.  If so, it, and all the pack
     * definitions, are loaded into the system.  A single jar file may contain more than
     * one pack if there are multiple definition files in it.  Alternately, a single jar
     * may contain a single pack, just with different directories of assets to load depending
     * on what mods and packs have been loaded alongside it.  No packs should be loaded between
     * the jar-checking code and the pack-loading code, as all possible packs and mods must
     * be loaded prior to trying to load the pack in case there are dependencies.
     */
    private static void checkJarForPacks(File packJar){
    	try{
	    	ZipFile jarFile = new ZipFile(packJar);
			Enumeration<? extends ZipEntry> entries = jarFile.entries();
			while(entries.hasMoreElements()){
				ZipEntry entry = entries.nextElement();
				if(entry.getName().endsWith("packdefinition.json")){
					JSONPack packDef = JSONParser.parseStream(new InputStreamReader(jarFile.getInputStream(entry), "UTF-8"), JSONPack.class, null, null);
					packJarMap.put(packDef.packID, packJar);
					packMap.put(packDef.packID, packDef);
				}
			}
			jarFile.close();
    	}catch(Exception e){
			InterfaceCore.logError("A fault was encountered when trying to check file " + packJar.getName() + " for pack data.  This pack will not be loaded.");
			e.printStackTrace();
		}
    } 
	
	/**
     * Called to load and parse all packs.  this must be done after the initial checking for
     * packs to ensure we see all possible dependencies.  This method checks to make
     * sure the pack's dependent parameters are valid and the pack can be loaded prior to
     * performing any actual loading operations.  Note that all packs in this routine
     * assume the default loader.  If you want to use a custom loader, you should manually
     * create and register your pack items and use {@link #registerItem(AJSONItem)}.
     */
    private static void parseAllPacks(){
    	List<String> packIDs = new ArrayList<String>(packMap.keySet());
    	Iterator<String> iterator = packMap.keySet().iterator();
    	while(iterator.hasNext()){
    		JSONPack packDef = packMap.get(iterator.next());
    		//Don't parse the core pack.  THat's all internal.
    		if(packDef.packID.equals(MasterLoader.resourceDomain)){
    			continue;
    		}
    		
    		//Create a listing of sub-directories we need to look in for pack definitions.
    		//These will be modified by activators or blockers.
    		List<String> validSubDirectories = new ArrayList<String>();
    		
    		//If we don't have any of the activating sets, don't load the pack. 
    		if(packDef.activators != null){
    			for(String subDirectory : packDef.activators.keySet()){
    				if(!packDef.activators.get(subDirectory).isEmpty()){
    					for(String activator : packDef.activators.get(subDirectory)){
    	    				if(packIDs.contains(activator) || InterfaceCore.isModPresent(activator)){
    	    					validSubDirectories.add(subDirectory);
        						break;
        					}
    	    			}
    				}else{
    					validSubDirectories.add(subDirectory);
    				}
    			}
    		}else{
    			validSubDirectories.add("");
    		}
    		
    		//If we have a blocking set, and we were going to load the pack, don't load it.
    		if(packDef.blockers != null){
    			for(String subDirectory : packDef.blockers.keySet()){
	    			for(String blocker : packDef.blockers.get(subDirectory)){
	    				if(packIDs.contains(blocker) || InterfaceCore.isModPresent(blocker)){
	    					validSubDirectories.remove(subDirectory);
    						break;
    					}
	    			}
    			}
    		}
    		
    		//If we have dependent sets, make sure we log a pack fault.
    		if(packDef.dependents != null){
    			for(String dependent : packDef.dependents){
					if(packIDs.contains(dependent) || InterfaceCore.isModPresent(dependent)){
						faultMap.put(packDef.packID, packDef.dependents);
						break;
					}
    			}
    		}
    		
    		//Load the pack components into the game.
    		//We iterate over all the sub-folders we found from the packDef checks.
    		PackStructure structure = PackStructure.values()[packDef.fileStructure];
    		for(String subDirectory : validSubDirectories){
	    		String assetPathPrefix = "assets/" + packDef.packID + "/";
				if(!subDirectory.isEmpty()){
					assetPathPrefix += subDirectory + "/";
				}
				
				try{
		    		ZipFile jarFile = new ZipFile(packJarMap.get(packDef.packID));
					Enumeration<? extends ZipEntry> entries = jarFile.entries();
					while(entries.hasMoreElements()){
						//Get next entry and path.
						ZipEntry entry = entries.nextElement();
						String entryFullPath = entry.getName();
						if(entryFullPath.startsWith(assetPathPrefix) && entryFullPath.endsWith(".json")){
							//JSON is in correct folder.  Get path properties and ensure they match our specs.
							//Need the asset folder structure between the main prefix and the asset itself.
							//This lets us know what asset we need to create as all assets are in their own folders.
							String fileName = entryFullPath.substring(entryFullPath.lastIndexOf('/') + 1);
							String assetPath = entryFullPath.substring(assetPathPrefix.length(), entryFullPath.substring(0, entryFullPath.length() - fileName.length()).lastIndexOf("/") + 1);
							if(!structure.equals(PackStructure.MODULAR)){
								//Need to trim the jsondefs folder to get correct sub-folder of jsondefs data.
								//Modular structure does not have a jsondefs folder, so we don't need to trim it off for that.
								//If we aren't modular, and aren't in a jsondefs folder, skip this entry.
								if(assetPath.startsWith("jsondefs/")){
									assetPath = assetPath.substring("jsondefs/".length());
								}else{
									continue;
								}
							}
							
							//Check to make sure json isn't an item JSON or our pack definition.
							if(!fileName.equals("packdefinition.json") && (structure.equals(PackStructure.MODULAR) ? !fileName.endsWith("_item.json") : entryFullPath.contains("jsondefs"))){
								//Get classification and JSON class type to use with GSON system.
								ItemClassification classification;
								try{
									classification = ItemClassification.fromDirectory(assetPath.substring(0, assetPath.indexOf("/") + 1));
								}catch(Exception e){
									InterfaceCore.logError("Was given an invalid classifcation sub-folder for asset: " + fileName + ".  Check your folder paths.");
									continue;
								}
								
								//Create the JSON instance.
								String systemName = fileName.substring(0, fileName.length() - ".json".length());
								AJSONItem definition;
								try{
									definition = JSONParser.parseStream(new InputStreamReader(jarFile.getInputStream(entry), "UTF-8"), classification.representingClass, packDef.packID, systemName);
								}catch(Exception e){
									InterfaceCore.logError("Could not parse: " + packDef.packID + ":" + fileName);
						    		InterfaceCore.logError(e.getMessage());
						    		continue;
								}
								
								//Remove the classification folder from the assetPath.  We don't use this for the resource-loading code.
								//Instead, this will be loaded by referencing the definition.  This also allows us to omit the path
								//if we are loading a non-default pack format.
								definition.packID = packDef.packID;
								definition.systemName = systemName;
								definition.classification = classification;
								definition.prefixFolders = assetPath.substring(classification.toDirectory().length()); 
								registerItem(definition);
							}
						}
					}
					
					//Done parsing.  Close the jarfile.
					jarFile.close();
				}catch(Exception e){
					InterfaceCore.logError("Could not start parsing of pack: " + packDef.packID);
					e.printStackTrace();
				}
    		}
    	}
    }
    
    /**
     * Called to add the passed-in item to the pack registry.  While this is normally called automatically by the
     * parser as it goes over the jar files, this may be called manually if other mods (or the core mod) want
     * to manually register things that aren't in jars.  The other assets like OBJs models and PNG textures 
     * must exist somewhere in a jar in the classpath, however.  This simply bypasses the requirement that the
     * JSON file exists.  For this reason, prefixFolders is able to be defined to specify where those assets are,
     * while resourceLoader is used to specify the loader to use to load those assets.
     * <br><br>
     * Note that no matter what method you you use, {@link AJSONItem#packID}, {@link AJSONItem#systemName},
     * {@link AJSONItem#classification}, and {@link AJSONItem#prefixFolders} MUST be set before calling this method.
     * <br><br>
     * Also note that any Legacy Compatibility code and JSON validation is performed prior to registration.
     * A fault in the compatibility system or in the validation will result in the item not being registered. 
     */
    public static void registerItem(AJSONItem itemDef){
    	try{
	    	//Do legacy compats before validating the JSON.
	    	//This will populate any required fields that were not in older versions.
			LegacyCompatSystem.performLegacyCompats(itemDef);
			JSONParser.validateFields(itemDef, itemDef.packID + ":" + itemDef.systemName + "/", 1);
	    			
	    	//Create all required items.
			if(itemDef instanceof AJSONMultiModelProvider){
				//Check if the definition is a skin.  If so, we need to just add it to the skin map for processing later.
				//We don't create skin items right away as the pack they go to might not yet be loaded.
				if(itemDef instanceof JSONSkin){
					JSONSkin skinDef = (JSONSkin) itemDef;
					if(!skinMap.containsKey(skinDef.skin.packID)){
						skinMap.put(skinDef.skin.packID, new HashMap<String, JSONSkin>());
					}
					skinMap.get(skinDef.skin.packID).put(skinDef.skin.systemName, skinDef);
				}else{
					parseAllDefinitions((AJSONMultiModelProvider) itemDef, ((AJSONMultiModelProvider) itemDef).definitions, itemDef.packID);
				}
			}else{
				AItemPack<?> item;
				switch(itemDef.classification){
					case INSTRUMENT : item = new ItemInstrument((JSONInstrument) itemDef); break;
					case ITEM : item = new ItemItem((JSONItem) itemDef); break;
					default : {
						throw new IllegalArgumentException("No corresponding classification found for asset: " + itemDef.prefixFolders + " Contact the mod author!");
					}
				}
		    	
		    	//Put the item in the map in the registry.
		    	if(!packItemMap.containsKey(item.definition.packID)){
		    		packItemMap.put(item.definition.packID, new HashMap<String, AItemPack<?>>());
		    	}
		    	packItemMap.get(item.definition.packID).put(item.definition.systemName, item);
			}
    	}catch(Exception e){
    		InterfaceCore.logError(e.getMessage());
    	}
    }
    
    /**
     * Called to load and parse all skins.  Skins are applied to existing pack vehicles in other packs if those
     * packs are loaded.  This is run after all packs are parsed to ensure that all pack definitions are loaded
     * prior to attempting to add skin definitions.
     */
    private static void parseAllSkins(){
    	for(String packID : skinMap.keySet()){
    		//Is the pack for this skin loaded?
    		if(packItemMap.containsKey(packID)){
    			//Check all skin items for the pack, and add them if they exist.
    			//The pack item map is keyed by the systemName plus the subName, so we can't
    			//just get the pack item with the systemName from that map.
    			//Since all items share the same definition file, if we change one definition
    			//we change all definitions, so only add the skins to the definition once.
    			for(String systemName : skinMap.get(packID).keySet()){
    				for(AItemPack<?> packItem : packItemMap.get(packID).values()){
    					if(packItem.definition.systemName.equals(systemName)){
    						//Parse and create all of the new definitions.
    						AJSONMultiModelProvider oldDefinition = (AJSONMultiModelProvider) packItem.definition;
    						List<JSONSubDefinition> newDefinitions = skinMap.get(packID).get(systemName).definitions;
    						parseAllDefinitions(oldDefinition, newDefinitions, skinMap.get(packID).get(systemName).packID);
    						
        					//Add the new definitions to the existing definitions of the existing item.
        					//This ensures the skins appear in the same tab as the existing item.
    						oldDefinition.definitions.addAll(newDefinitions);
        					break;
    					}
    				}
    			}
    		}
    	}
    }
    
    /**
     * Helper method to parse multi-definition pack items.
     * Generated items are added to the passed-in list.
     */
    private static void parseAllDefinitions(AJSONMultiModelProvider mainDefinition, List<JSONSubDefinition> subDefinitions, String sourcePackID){
    	Map<String, AItemPack<?>> packItems = new HashMap<String, AItemPack<?>>();
    	for(JSONSubDefinition subDefinition : subDefinitions){
			AItemPack<?> item = null;
			switch(mainDefinition.classification){
				case VEHICLE : item = new ItemVehicle((JSONVehicle) mainDefinition, subDefinition.subName, sourcePackID); break;
				case PART : {
					JSONPart partDef = (JSONPart) mainDefinition;
					for(AItemPartCreator creator : partCreators){
						if(creator.isCreatorValid(partDef)){
							item = creator.createItem(partDef, subDefinition.subName, sourcePackID);
							break;
						}
					}
					if(item == null){
						InterfaceCore.logError("Was told to parse part " + partDef.packID + ":" + partDef.systemName + " with part type " + partDef.generic.type + ", but that's not a valid type for creating a part.");
						return;
					}
					break;
				}
				case DECOR : item = new ItemDecor((JSONDecor) mainDefinition, subDefinition.subName, sourcePackID); break;
				case POLE : item = new ItemPoleComponent((JSONPoleComponent) mainDefinition, subDefinition.subName, sourcePackID); break;
				case ROAD : item = new ItemRoadComponent((JSONRoadComponent) mainDefinition, subDefinition.subName, sourcePackID); break;
				case BULLET : item = new ItemBullet((JSONBullet) mainDefinition, subDefinition.subName, sourcePackID); break;
				default : {
					throw new IllegalArgumentException("A classification for a normal item is trying to register as a multi-model provider.  This is an error in the core mod.  Contact the mod author.  Asset being loaded is: " + mainDefinition.packID + ":" + mainDefinition.systemName);
				}
			}
			
			//Add the pack item to the map.  We need to make sure all subDefinitions
			//are okay before adding the entire definition.
			packItems.put(item.definition.systemName + subDefinition.subName, item);
		}
    	
    	//All definitions were okay.  Add items to the registry.
    	if(!packItemMap.containsKey(mainDefinition.packID)){
    		packItemMap.put(mainDefinition.packID, new HashMap<String, AItemPack<?>>());
    	}
    	packItemMap.get(mainDefinition.packID).putAll(packItems);
    }
    
    /**
     * Called after adding all items to a pack to sort it.
     * This is normally called internally here after creating all items,
     * but may be called by external loaders that need to sort after
     * adding their items.
     */
    public static void sortPackItems(String packID){
    	List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
    	HashMap<String, AItemPack<?>> packSpecificItemMap = packItemMap.get(packID);
    	packItems.addAll(packSpecificItemMap.values());
    	packItems.sort(packComparator);
    	packSpecificItemMap.clear();
    	for(AItemPack<?> packItem : packItems){
    		if(packItem.definition instanceof AJSONMultiModelProvider){
    			packSpecificItemMap.put(packItem.definition.systemName, packItem);
    		}else{
    			packSpecificItemMap.put(packItem.definition.systemName, packItem);
    		}
    	}
    }
    
    
    //--------------------START OF HELPER METHODS--------------------
    public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem> PackItem getItem(String packID, String systemName){
    	return getItem(packID, systemName, "");
    }
    
    @SuppressWarnings("unchecked")
	public static <PackItem extends AItemPack<JSONDefinition>, JSONDefinition extends AJSONItem> PackItem getItem(String packID, String systemName, String subName){
    	if(packItemMap.containsKey(packID)){
    		return (PackItem) packItemMap.get(packID).get(systemName + subName);
    	}
    	return null;
    }
    
    public static boolean arePacksPresent(){
    	//We always have 1 pack: the core pack.
    	return packItemMap.size() > 1;
    }
    
    public static Set<String> getAllPackIDs(){
    	return packItemMap.keySet();
    }
    
    public static JSONPack getPackConfiguration(String packID){
    	return packMap.get(packID);
    }
    
    public static List<AItemPack<?>> getAllItemsForPack(String packID, boolean sorted){
    	List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>(packItemMap.get(packID).values());
    	if(sorted){
    		packItems.sort(packComparator);
    	}
    	return packItems;
    }
    
    public static List<AItemPack<?>> getAllPackItems(){
    	List<AItemPack<?>> packItems = new ArrayList<AItemPack<?>>();
    	for(String packID : packItemMap.keySet()){
    		packItems.addAll(getAllItemsForPack(packID, false));
    	}
    	return packItems;
    }
}
