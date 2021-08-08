package minecrafttransportsimulator.entities.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import minecrafttransportsimulator.baseclasses.Orientation3d;
import minecrafttransportsimulator.baseclasses.Point3d;
import minecrafttransportsimulator.entities.instances.APart;
import minecrafttransportsimulator.entities.instances.EntityParticle;
import minecrafttransportsimulator.entities.instances.EntityVehicleF_Physics;
import minecrafttransportsimulator.items.components.AItemPack;
import minecrafttransportsimulator.items.components.AItemSubTyped;
import minecrafttransportsimulator.jsondefs.AJSONMultiModelProvider;
import minecrafttransportsimulator.jsondefs.JSONAnimatedObject;
import minecrafttransportsimulator.jsondefs.JSONAnimationDefinition;
import minecrafttransportsimulator.jsondefs.JSONCameraObject;
import minecrafttransportsimulator.jsondefs.JSONLight;
import minecrafttransportsimulator.jsondefs.JSONParticle;
import minecrafttransportsimulator.jsondefs.JSONSound;
import minecrafttransportsimulator.jsondefs.JSONSubDefinition;
import minecrafttransportsimulator.jsondefs.JSONText;
import minecrafttransportsimulator.mcinterface.InterfaceClient;
import minecrafttransportsimulator.mcinterface.InterfaceRender;
import minecrafttransportsimulator.mcinterface.WrapperNBT;
import minecrafttransportsimulator.mcinterface.WrapperWorld;
import minecrafttransportsimulator.rendering.components.ARenderEntity;
import minecrafttransportsimulator.rendering.components.DurationDelayClock;
import minecrafttransportsimulator.sound.InterfaceSound;
import minecrafttransportsimulator.sound.SoundInstance;
import minecrafttransportsimulator.systems.CameraSystem;
import minecrafttransportsimulator.systems.PackParserSystem;

/**Base class for entities that are defined via JSON definitions and can be modeled in 3D.
 * This level adds various method for said definitions, which include rendering functions. 
 * 
 * @author don_bruce
 */
public abstract class AEntityC_Definable<JSONDefinition extends AJSONMultiModelProvider> extends AEntityB_Existing{
	/**Map of created entities that can be rendered in the world, including those without {@link #lookupID}s.**/
	private static final Map<WrapperWorld, LinkedHashSet<AEntityC_Definable<?>>> renderableEntities = new HashMap<WrapperWorld, LinkedHashSet<AEntityC_Definable<?>>>();
	
	/**The pack definition for this entity.  May contain extra sections if the super-classes
	 * have them in their respective JSONs.
	 */
	public final JSONDefinition definition;

	/**The current subName for this entity.  Used to select which definition represents this entity.*/
	public String subName;
	
	/**Variable for saving animation initialized state.  Is set true on the first tick, but may be set false afterwards to re-initialize animations.*/
	public boolean animationsInitialized;
	
	/**Map containing text lines for saved text provided by this entity.**/
	public final LinkedHashMap<JSONText, String> text = new LinkedHashMap<JSONText, String>();
	
	/**Set of variables that are "on" for this entity.  Used for animations.**/
	public final Set<String> variablesOn = new HashSet<String>();
	
	private final List<JSONSound> allSoundDefs = new ArrayList<JSONSound>();
	private final Map<JSONSound, List<DurationDelayClock>> soundActiveClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONSound, List<DurationDelayClock>> soundVolumeClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONSound, List<DurationDelayClock>> soundPitchClocks = new HashMap<JSONSound, List<DurationDelayClock>>();
	private final Map<JSONLight, List<DurationDelayClock>> lightBrightnessClocks = new HashMap<JSONLight, List<DurationDelayClock>>();
	private final Map<JSONParticle, List<DurationDelayClock>> particleActiveClocks = new HashMap<JSONParticle, List<DurationDelayClock>>();
	private final Map<JSONParticle, Long> lastTickParticleSpawned = new HashMap<JSONParticle, Long>();
	
	/**Maps rendering animations to their respective clocks.  Used only for the rendering JSON section.**/
	public final Map<JSONAnimationDefinition, DurationDelayClock> renderAnimationClocks = new HashMap<JSONAnimationDefinition, DurationDelayClock>();
	
	/**Maps camera animations to their respective clocks.  Used only for the camera JSON section.**/
	public final Map<JSONAnimationDefinition, DurationDelayClock> cameraAnimationClocks = new HashMap<JSONAnimationDefinition, DurationDelayClock>();
	
	/**Maps light definitions to their current brightness.  This is updated every frame prior to rendering.**/
	public final Map<JSONLight, Float> lightBrightnessValues = new HashMap<JSONLight, Float>();
	
	/**Constructor for synced entities**/
	public AEntityC_Definable(WrapperWorld world, WrapperNBT data){
		super(world, data);
		//Set definition and current subName.
		//TODO remove forwarding in V21.
		this.subName = data.getString("subName");
		if(subName.isEmpty()){
			subName = data.getString("currentSubName");
		}
		AItemSubTyped<JSONDefinition> item = PackParserSystem.getItem(data.getString("packID"), data.getString("systemName"), subName);
		if(item != null){
			this.definition = item.definition;
			//this.subName = item.subName;
		}else{
			this.definition = generateDefaultDefinition();
			//this.subName = "";
		}
		
		//Load text.
		if(definition.rendering != null && definition.rendering.textObjects != null){
			for(int i=0; i<definition.rendering.textObjects.size(); ++i){
				text.put(definition.rendering.textObjects.get(i), data.getString("textLine" + i));
			}
		}
		
		//Load variables.
		this.variablesOn.addAll(data.getStrings("variablesOn"));
		
		if(definition.rendering != null && definition.rendering.constants != null){
			variablesOn.addAll(definition.rendering.constants);
		}
	}
	
	/**Constructor for un-synced entities.  Allows for specification of position/motion/angles.**/
	public AEntityC_Definable(WrapperWorld world, Point3d position, Point3d motion, Orientation3d angles, AItemSubTyped<JSONDefinition> creatingItem){
		super(world, position, motion, angles);
		this.subName = creatingItem.subName;
		this.definition = creatingItem.definition;
		
		//Add constants.
		if(definition.rendering != null && definition.rendering.constants != null){
			variablesOn.addAll(definition.rendering.constants);
		}
	}
	
	@Override
	public boolean update(){
		if(super.update()){
			if(!animationsInitialized){
				initializeAnimations();
				animationsInitialized = true;
			}
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Call to get all renderable entities from the world.  This includes
	 * both tracked and un-tracked entities.  This list may be null on the
	 * first frame before any entities have been spawned, and entities
	 * may be removed from this list at any time, so watch out for CMEs!
	 * Note that this listing is a linked hash set, so iteration will be
	 * in the same order entities were added.
	 * 
	 */
	public static LinkedHashSet<AEntityC_Definable<?>> getRenderableEntities(WrapperWorld world){
		return renderableEntities.get(world);
	}
	
	/**
	 * Call this if you need to remove all entities from the world.  Used mainly when
	 * a world is un-loaded because no players are in it anymore.
	 */
	public static void removaAllEntities(WrapperWorld world){
		LinkedHashSet<AEntityC_Definable<?>> existingEntities = renderableEntities.get(world);
		if(existingEntities != null){
			//Need to copy the entities so we don't CME the map keys.
			LinkedHashSet<AEntityA_Base> entities = new LinkedHashSet<AEntityA_Base>();
			entities.addAll(existingEntities);
			for(AEntityA_Base entity : entities){
				entity.remove();
			}
			renderableEntities.remove(world);
		}
	}
	
	/**
	 *  Called the first update tick after this entity is first constructed, and when the definition on it is reset via hotloading.
	 *  This should create (and reset) all animation clocks and other static objects that depend on the definition. 
	 */
	protected void initializeAnimations(){
		//Add us to the entity rendering list.
		LinkedHashSet<AEntityC_Definable<?>> worldEntities = renderableEntities.get(world);
		if(worldEntities == null){
			worldEntities = new LinkedHashSet<AEntityC_Definable<?>>();
			renderableEntities.put(world, worldEntities);
		}
		worldEntities.add(this);
		
		allSoundDefs.clear();
		soundActiveClocks.clear();
		soundVolumeClocks.clear();
		soundPitchClocks.clear();
		if(definition.rendering != null && definition.rendering.sounds != null){
			for(JSONSound soundDef : definition.rendering.sounds){
				allSoundDefs.add(soundDef);
				
				List<DurationDelayClock> activeClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.activeAnimations){
						activeClocks.add(new DurationDelayClock(animation));
					}
				}
				soundActiveClocks.put(soundDef, activeClocks);
				
				List<DurationDelayClock> volumeClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.volumeAnimations !=  null){
					for(JSONAnimationDefinition animation : soundDef.volumeAnimations){
						volumeClocks.add(new DurationDelayClock(animation));
					}
				}
				soundVolumeClocks.put(soundDef, volumeClocks);
				
				List<DurationDelayClock> pitchClocks = new ArrayList<DurationDelayClock>();
				if(soundDef.pitchAnimations != null){
					for(JSONAnimationDefinition animation : soundDef.pitchAnimations){
						pitchClocks.add(new DurationDelayClock(animation));
					}
				}
				soundPitchClocks.put(soundDef, pitchClocks);
			}
		}
		
		lightBrightnessClocks.clear();
		lightBrightnessValues.clear();
		if(definition.rendering != null && definition.rendering.lightObjects != null){
			for(JSONLight lightDef : definition.rendering.lightObjects){
				List<DurationDelayClock> lightClocks = new ArrayList<DurationDelayClock>();
				if(lightDef.brightnessAnimations !=  null){
					for(JSONAnimationDefinition animation : lightDef.brightnessAnimations){
						lightClocks.add(new DurationDelayClock(animation));
					}
				}
				lightBrightnessClocks.put(lightDef, lightClocks);
				lightBrightnessValues.put(lightDef, 0F);
			}
		}
		
		particleActiveClocks.clear();
		if(definition.rendering != null && definition.rendering.particles != null){
			for(JSONParticle particleDef : definition.rendering.particles){
				List<DurationDelayClock> activeClocks = new ArrayList<DurationDelayClock>();
				if(particleDef.activeAnimations !=  null){
					for(JSONAnimationDefinition animation : particleDef.activeAnimations){
						activeClocks.add(new DurationDelayClock(animation));
					}
				}
				particleActiveClocks.put(particleDef, activeClocks);
				lastTickParticleSpawned.put(particleDef, ticksExisted);
			}
		}
		
		renderAnimationClocks.clear();
		if(definition.rendering != null){
			if(definition.rendering.animatedObjects != null){
				for(JSONAnimatedObject animatedObject : definition.rendering.animatedObjects){
					if(animatedObject.animations != null){
						for(JSONAnimationDefinition animation : animatedObject.animations){
							renderAnimationClocks.put(animation, new DurationDelayClock(animation));
						}
					}
				}
			}
		}
		
		cameraAnimationClocks.clear();
		if(definition.rendering != null){
			if(definition.rendering.cameraObjects != null){
				for(JSONCameraObject cameraObject : definition.rendering.cameraObjects){
					if(cameraObject.animations != null){
						for(JSONAnimationDefinition animation : cameraObject.animations){
							cameraAnimationClocks.put(animation, new DurationDelayClock(animation));
						}
					}
				}
			}
		}
	}
	
	@Override
	public void remove(){
		super.remove();
		//Need to check for null, as this key may not exist if we were an entity spawned in a world but never ticked.
		LinkedHashSet<AEntityC_Definable<?>> entities = renderableEntities.get(world);
		if(entities != null){
			renderableEntities.get(world).remove(this);
		}
	}
	
	/**
	 *  Returns the current item for this entity.
	 */
	public <ItemInstance extends AItemPack<JSONDefinition>> ItemInstance getItem(){
		return PackParserSystem.getItem(definition.packID, definition.systemName, subName);
	}
	
	/**
	 *  Generates the default definition for this entity. Used if the item can't be found.
	 *  This allows for internally-definable entities.
	 */
	public JSONDefinition generateDefaultDefinition(){
		throw new IllegalArgumentException("Was asked to auto-generate a definition on an entity with one not defined.  This is NOT allowed.  The entity must be missing its item.  Perhaps a pack was removed with this entity still in the world?");
	}
	
    /**
   	 *  Returns true if this entity is lit up, and text should be rendered lit.
   	 *  Note that what text is lit is dependent on the text's definition, so just
   	 *  because text could be lit, does not mean it will be lit if the pack
   	 *  author doesn't want it to be.
   	 */
    public boolean renderTextLit(){
    	return true;
    }
    
    /**
   	 *  Returns a string that represents this entity's secondary text color.  If this color is set,
   	 *  and text is told to render from this provider, and that text is told to use this color, then it will.
   	 *  Otherwise, the text will use its default color.
   	 */
    public String getSecondaryTextColor(){
    	for(JSONSubDefinition subDefinition : definition.definitions){
			if(subDefinition.subName.equals(subName)){
				return subDefinition.secondColor;
			}
		}
		throw new IllegalArgumentException("Tried to get the definition for an object of subName:" + subName + ".  But that isn't a valid subName for the object:" + definition.packID + ":" + definition.systemName + ".  Report this to the pack author as this is a missing JSON component!");
    }
    
    /**
	 *  Called to update the text on this entity.  Normally just sets the text to the passed-in values,
	 *  but may do supplemental logic if desired.
	 */
    public void updateText(List<String> textLines){
    	int linesChecked = 0;
		for(Entry<JSONText, String> textEntry : text.entrySet()){
			textEntry.setValue(textLines.get(linesChecked));
			++linesChecked;
		}
    }
    
    /**
   	 *  Spawns particles for this entity.  This is called after every render frame, so
   	 *  watch your methods to prevent spam.  Note that this method is not called if the
   	 *  game is paused, as particles are assumed to only be spawned during normal entity
   	 *  updates.
   	 */
    public void spawnParticles(float partialTicks){
    	//Check all particle defs and update the existing particles accordingly.
    	for(Entry<JSONParticle, List<DurationDelayClock>> particleEntry : particleActiveClocks.entrySet()){
    		JSONParticle particleDef = particleEntry.getKey();
    		//Check if the particle should be spawned this tick.
    		boolean shouldParticleSpawn = true;
			boolean anyClockMovedThisUpdate = false;
			if(particleDef.activeAnimations != null){
				boolean inhibitAnimations = false;
				for(DurationDelayClock clock : particleEntry.getValue()){
					switch(clock.animation.animationType){
						case VISIBILITY :{
							//We use the clock here to check if the state of the variable changed, not
							//to clamp the value used in the testing.
							if(!inhibitAnimations){
								double variableValue = clock.animation.offset + getAnimatedVariableValue(clock, 0, partialTicks);
								if(!anyClockMovedThisUpdate){
									anyClockMovedThisUpdate = clock.movedThisUpdate;
								}
								if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
									shouldParticleSpawn = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, partialTicks);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, partialTicks);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case TRANSLATION :{
							//Do nothing.
							break;
						}
						case ROTATION :{
							//Do nothing.
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
					}
					
					if(!shouldParticleSpawn){
						//Don't need to process any further as we can't spawn.
						break;
					}
				}
			}
			
			//Make the particle spawn if able.
			if(shouldParticleSpawn && (anyClockMovedThisUpdate || (particleDef.spawnEveryTick && ticksExisted > lastTickParticleSpawned.get(particleDef)))){
				lastTickParticleSpawned.put(particleDef, ticksExisted);
				if(particleDef.quantity > 0){
					for(int i=0; i<particleDef.quantity; ++i){
						InterfaceRender.spawnParticle(new EntityParticle(this, particleDef));
					}
				}else{
					InterfaceRender.spawnParticle(new EntityParticle(this, particleDef));
				}
			}
    	}
    }
    
    /**
   	 *  Updates the light brightness values contained in {@link #lightBrightnessValues}.  This is done
   	 *  every frame for all light definitions to prevent excess calculations caused by multiple
   	 *  lighting components for the light re-calculating the same value multiple times a frame.
   	 *  An example of this is a light with a bean and flare component. 
   	 */
    public void updateLightBrightness(float partialTicks){
		for(JSONLight lightObject : lightBrightnessClocks.keySet()){
			boolean definedBrightness = false;
			float lightLevel = 0.0F;
			boolean inhibitAnimations = false;
			boolean inhibitLight = false;
			for(DurationDelayClock clock : lightBrightnessClocks.get(lightObject)){
				switch(clock.animation.animationType){
					case VISIBILITY :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0, partialTicks);
							if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
								inhibitLight = true;
							}
						}
						break;
					}
					case INHIBITOR :{
						if(!inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0, partialTicks);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
								inhibitAnimations = true;
							}
						}
						break;
					}
					case ACTIVATOR :{
						if(inhibitAnimations){
							double variableValue = getAnimatedVariableValue(clock, 0, partialTicks);
							if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
								inhibitAnimations = false;
							}
						}
						break;
					}
					case TRANSLATION :{
						if(!inhibitAnimations){
							if(clock.animation.axis.axis.x != 0){
								lightLevel *= getAnimatedVariableValue(clock, 0, partialTicks);
							}else if(clock.animation.axis.axis.y != 0){
								lightLevel += getAnimatedVariableValue(clock, 0, partialTicks);
							}else{
								lightLevel = (float) getAnimatedVariableValue(clock, 0, partialTicks);
							}
						}
						break;
					}
					case ROTATION :{
						//Do nothing.
						break;
					}
					case SCALING :{
						//Do nothing.
						break;
					}
				}
				if(inhibitLight){
					//No need to process further.
					break;
				}
			}
			if(inhibitLight || lightLevel < 0){
				lightLevel = 0;
			}else if(!definedBrightness || lightLevel > 1){
				lightLevel = 1;
			}
			lightBrightnessValues.put(lightObject, lightLevel);
		}
    }
	
	/**
	 *  Returns the raw value for the passed-in variable.  If the variable is not present, NaM
	 *  should be returned (calling functions need to account for this!).
	 *  This should be extended on all sub-classes for them to provide their own variables.
	 *  For all cases of this, the sub-classed variables should be checked first.  If none are
	 *  found, then the super() method should be called to return those as a default.
	 */
	public double getRawVariableValue(String variable, float partialTicks){
		switch(variable){
			case("tick"): return world.getTick() + partialTicks;
			case("tick_sin"): return Math.sin(Math.toRadians(world.getTick() + partialTicks));
			case("tick_cos"): return Math.cos(Math.toRadians(world.getTick() + partialTicks));
			case("time"): return world.getTime();
			case("rain_strength"): return (int) world.getRainStrength(position);
			case("rain_sin"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.sin(rainStrength*Math.toRadians(360*(world.getTick() + partialTicks)/20))/2D + 0.5: 0;
			}
			case("rain_cos"): {
				int rainStrength = (int) world.getRainStrength(position); 
				return rainStrength > 0 ? Math.cos(rainStrength*Math.toRadians(360*(world.getTick() + partialTicks)/20))/2D + 0.5 : 0;
			}	
			case("light_sunlight"): return world.getLightBrightness(position, false);
			case("light_total"): return world.getLightBrightness(position, true);
			case("ground_distance"): return world.getHeight(position);
		}
		
		//Check if this is a cycle variable.
		if(variable.endsWith("_cycle")){
			String[] parsedVariable = variable.split("_");
			int offTime = Integer.valueOf(parsedVariable[0]);
			int onTime = Integer.valueOf(parsedVariable[1]);
			int totalTime = offTime + onTime + Integer.valueOf(parsedVariable[2]);
			long timeInCycle = world.getTick()%totalTime;
			return timeInCycle > offTime && timeInCycle - offTime < onTime ? 1 : 0;
		}
		
		//Check if this is a generic variable.  This contains lights in most cases.
		if(variablesOn.contains(variable)){
			return 1;
		}
		
		//Didn't find a variable.  Return NaN.
		return Double.NaN;
	}
	
	/**
	 *  Returns the value for the passed-in variable, subject to the clamping, and duration/delay requested in the 
	 *  animation definition.  The passed-in offset is used to allow for stacking animations, and should be 0 if 
	 *  this functionality is not required.  The passed-in clock may be null to prevent duration/delay functionality.
	 *  Returns the value of the variable, or 0 if the variable is not valid.
	 */
	public final double getAnimatedVariableValue(DurationDelayClock clock, double offset, float partialTicks){
		double value = getRawVariableValue(clock.animation.variable, partialTicks);
		if(Double.isNaN(value)){
			value = 0;
		}
		if(!clock.isUseful){
			return clampAndScale(value, clock.animation, offset);
		}else{
			return clampAndScale(clock.getFactoredState(this, value), clock.animation, offset);
		}
	}
	
	/**
	 *  Helper method to clamp and scale the passed-in variable value based on the passed-in animation, 
	 *  returning it in the proper form.
	 */
	private static double clampAndScale(double value, JSONAnimationDefinition animation, double offset){
		if(animation.axis != null){
			value = animation.axis.rotationZ*(animation.absolute ? Math.abs(value) : value) + animation.offset + offset;
			if(animation.clampMin != 0 && value < animation.clampMin){
				value = animation.clampMin;
			}else if(animation.clampMax != 0 && value > animation.clampMax){
				value = animation.clampMax;
			}
		}
		return animation.absolute ? Math.abs(value) : value;
	}
	
	/**
	 *  Helper method to get the index of the passed-in variable.  Indexes are defined by
	 *  variable names ending in _xx, where xx is a number.  The defined number is assumed
	 *  to be 1-indexed, but the returned number will be 0-indexed.  If the variable doesn't
	 *  define a number, then -1 is returned.
	 */
	public static int getVariableNumber(String variable){
		if(variable.matches("^.*_[0-9]+$")){
			return Integer.parseInt(variable.substring(variable.lastIndexOf('_') + 1)) - 1;
		}else{
			return -1;
		}
	}
    
    /**
	 *  Gets the renderer for this entity.  No actual rendering should be done in this method, 
	 *  as doing so could result in classes being imported during object instantiation on the server 
	 *  for graphics libraries that do not exist.  Instead, generate a class that does this and call it.
	 *  This method is assured to be only called on clients, so you can just do the construction of the
	 *  renderer in this method and pass it back as the return.
	 */
	public abstract <RendererInstance extends ARenderEntity<AnimationEntity>, AnimationEntity extends AEntityC_Definable<?>> RendererInstance getRenderer();
    
    @Override
    public void updateSounds(){
    	super.updateSounds();
    	//Check all sound defs and update the existing sounds accordingly.
    	for(JSONSound soundDef : allSoundDefs){
    		//Check if the sound should be playing before we try to update state.
    		AEntityD_Interactable<?> entityRiding = InterfaceClient.getClientPlayer().getEntityRiding();
    		boolean playerRidingEntity = this.equals(entityRiding) || (this instanceof APart && ((APart) this).entityOn.equals(entityRiding));
    		boolean shouldSoundPlay = playerRidingEntity && InterfaceClient.inFirstPerson() && !CameraSystem.areCustomCamerasActive() ? !soundDef.isExterior : !soundDef.isInterior;
			boolean anyClockMovedThisUpdate = false;
			boolean inhibitAnimations = false;
			if(shouldSoundPlay){
				for(DurationDelayClock clock : soundActiveClocks.get(soundDef)){
					switch(clock.animation.animationType){
						case VISIBILITY :{
							//We use the clock here to check if the state of the variable changed, not
							//to clamp the value used in the testing.
							if(!inhibitAnimations){
								double variableValue = clock.animation.offset + getAnimatedVariableValue(clock, 0, 0);
								if(!anyClockMovedThisUpdate){
									anyClockMovedThisUpdate = clock.movedThisUpdate;
								}
								if(variableValue < clock.animation.clampMin || variableValue > clock.animation.clampMax){
									shouldSoundPlay = false;
								}
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case TRANSLATION :{
							//Do nothing.
							break;
						}
						case ROTATION :{
							//Do nothing.
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
					}
					
					if(!shouldSoundPlay){
						//Don't need to process any further as we can't play.
						break;
					}
				}
			}
			
			//If we aren't a looping or repeating sound, check if we had a clock-movement to trigger us.
			//If we didn't, then we shouldn't play, even if all states are true.
			if(!soundDef.looping && !soundDef.forceSound && !anyClockMovedThisUpdate){
				shouldSoundPlay = false;
			}
			
			if(shouldSoundPlay){
				//Sound should play.  If it's not playing, start it.
				boolean isSoundPlaying = false;
				if(!soundDef.forceSound){
					for(SoundInstance sound : sounds){
						if(sound.soundName.equals(soundDef.name)){
							isSoundPlaying = true;
							break;
						}
					}
				}
				if(!isSoundPlaying){
					InterfaceSound.playQuickSound(new SoundInstance(this, soundDef.name, soundDef.looping));
				}
			}else{
				if(soundDef.looping){
					//If sound is playing, stop it.
					for(SoundInstance sound : sounds){
						if(sound.soundName.equals(soundDef.name)){
							sound.stopSound = true;
							break;
						}
					}
				}
				
				//Go to the next soundDef.  No need to change properties on sounds that shouldn't play.
				continue;
			}
			
			//Sound should be playing.  If it's part of the sound list, update properties.
			//Sounds may not be in the list if they have just been queued and haven't started yet.
			SoundInstance sound = null;
			for(SoundInstance activeSound : sounds){
				if(activeSound.soundName.equals(soundDef.name)){
					sound = activeSound;
					break;
				}
			}
			
			if(sound != null){
				//Adjust volume.
				boolean definedVolume = false;
				inhibitAnimations = false;
				sound.volume = 0;
				for(DurationDelayClock clock : soundVolumeClocks.get(soundDef)){
					switch(clock.animation.animationType){
						case TRANSLATION :{
							if(!inhibitAnimations){
								definedVolume = true;
								sound.volume += Math.signum(clock.animation.axis.axis.y)*getAnimatedVariableValue(clock, -clock.animation.offset, 0) + clock.animation.offset;
							}
							break;
						}
						case ROTATION :{
							if(!inhibitAnimations){
								definedVolume = true;
								double parabolaValue = Math.signum(clock.animation.axis.axis.y)*getAnimatedVariableValue(clock, -clock.animation.offset, 0);
								sound.volume += clock.animation.axis.axis.x*Math.pow(parabolaValue - clock.animation.axis.axis.z, 2) + clock.animation.offset;
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
						case VISIBILITY :{
							//Do nothing.
							break;
						}
					}
				}
				if(!definedVolume){
					sound.volume = 1;
				}else if(sound.volume < 0){
					sound.volume = 0;
				}
				
				//If the player is in a closed-top vehicle that isn't this one, dampen the sound
				//Unless it's a radio, in which case don't do so.
				if(!playerRidingEntity && sound.radio == null && entityRiding instanceof EntityVehicleF_Physics && !((EntityVehicleF_Physics) entityRiding).definition.motorized.hasOpenTop && InterfaceClient.inFirstPerson() && !CameraSystem.areCustomCamerasActive()){
					sound.volume *= 0.5F;
				}
				
				//Adjust pitch.
				boolean definedPitch = false;
				inhibitAnimations = false;
				sound.pitch = 0;
				for(DurationDelayClock clock : soundPitchClocks.get(soundDef)){
					switch(clock.animation.animationType){
						case TRANSLATION :{
							if(!inhibitAnimations){
								definedPitch = true;
								sound.pitch += Math.signum(clock.animation.axis.axis.y)*getAnimatedVariableValue(clock, -clock.animation.offset, 0) + clock.animation.offset;
							}
							break;
						}
						case ROTATION :{
							if(!inhibitAnimations){
								definedPitch = true;
								double parabolaValue = Math.signum(clock.animation.axis.axis.y)*getAnimatedVariableValue(clock, -clock.animation.offset, 0);
								sound.pitch += clock.animation.axis.axis.x*Math.pow(parabolaValue - clock.animation.axis.axis.z, 2) + clock.animation.offset;
							}
							break;
						}
						case INHIBITOR :{
							if(!inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = true;
								}
							}
							break;
						}
						case ACTIVATOR :{
							if(inhibitAnimations){
								double variableValue = getAnimatedVariableValue(clock, 0, 0);
								if(variableValue >= clock.animation.clampMin && variableValue <= clock.animation.clampMax){
									inhibitAnimations = false;
								}
							}
							break;
						}
						case SCALING :{
							//Do nothing.
							break;
						}
						case VISIBILITY :{
							//Do nothing.
							break;
						}
					}
				}
				if(!definedPitch){
					sound.pitch = 1;
				}else if(sound.pitch < 0){
					sound.pitch = 0;
				}
			}
    	}
    }
	
	@Override
	public WrapperNBT save(WrapperNBT data){
		super.save(data);
		data.setString("packID", definition.packID);
		data.setString("systemName", definition.systemName);
		data.setString("subName", subName);
		int lineNumber = 0;
		for(String textLine : text.values()){
			data.setString("textLine" + lineNumber++, textLine);
		}
		data.setStrings("variablesOn", variablesOn);
		return data;
	}
}
