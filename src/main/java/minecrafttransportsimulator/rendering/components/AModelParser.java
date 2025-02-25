package minecrafttransportsimulator.rendering.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import minecrafttransportsimulator.entities.components.AEntityC_Definable;
import minecrafttransportsimulator.rendering.instances.ModelParserOBJ;

/**Abstract class for parsing models.  This contains methods for determining what models
 * the parser can parse, and the operations for parsing them into the form MTS needs.
 * It also stores a list of created parsers for use when requesting a model be parsed.
 * By default, an OBJ parser is created when this class is first accessed, but one may
 * add other parsers as they see fit.
 * 
 * @author don_bruce
 */
public abstract class AModelParser{
	private static final Map<String, AModelParser> parsers = new HashMap<String, AModelParser>();
	public static final String ROLLER_OBJECT_NAME = "roller";
	public static final String WINDOW_OBJECT_NAME = "window";
	public static final String ONLINE_TEXTURE_OBJECT_NAME = "url";
	public static final String TRANSLUCENT_OBJECT_NAME = "translucent";
	public static final String INTERIOR_WINDOW_SUFFIX = "_autogen_interior";
	
	public AModelParser(){
		parsers.put(getModelSuffix(), this);
	}
	
	static{
		new ModelParserOBJ();
	}

	/**
	 *  Returns the model file suffix for the model that can be parsed by this parser.
	 *  If a parser is added with the same suffix as an existing parser, it is replaced.
	 *  The suffix returned should be the file-extension portion after the dot.
	 */
	protected abstract String getModelSuffix();
	
	/**
	 *  Parses the model at the passed-in location. The return value is a map, with the String
	 *  key being the name of an object on the model, and the value being a two-dimensional array
	 *  of floats.  Each element of the first dimension is a vertex.  Therefore, the second dimension
	 *  are the floats that make up that vertex's properties.  They are in the order as follows:
	 *  <ul>
	 *  <li>The x-coordinate of a vertex on the model.
	 *  <li>The y-coordinate of a vertex on the model.
	 *  <li>The z-coordinate of a vertex on the model.
	 *  <li>The u-coordinate of the UV-mapping for that vertex.
	 *  <li>The v-coordinate of the UV-mapping for that vertex.
	 *  <li>The nX-coordinate of the normal for that vertex, in the x-dimension.
	 *  <li>The nY-coordinate of the normal for that vertex, in the y-dimension.
	 *  <li>The nZ-coordinate of the normal for that vertex, in the z-dimension.
	 *  </ul>
	 */
	protected abstract Map<String, Float[][]> parseModelInternal(String modelLocation);
	
	/**
	 *  Attempts to obtain the parser for the passed-in modelLocation.  After this, the model
	 *  is parsed and returned.  If no parser is found, an exception is thrown.
	 */
	public static Map<String, Float[][]> parseModel(String modelLocation){
		AModelParser parser = parsers.get(modelLocation.substring(modelLocation.lastIndexOf(".") + 1));
		if(parser != null){
			return parser.parseModelInternal(modelLocation);
		}else{
			throw new IllegalArgumentException("No parser found for model format of " + modelLocation.substring(modelLocation.lastIndexOf(".") + 1));
		}
	}
	
	/**
	 *  Parses the model at the passed-in location, and generates all {@link RenderableModelObject}s for it.
	 *  These are returned as a list.  Objects in the parsed model are cross-checked with the passed-in 
	 *  definition to ensure the proper constructors are created.  All objects in the model
	 *  are assured to be turned into one of the objects in the returned list.
	 *  The passed-in definition may be null to prevent checking against JSON, though non-JSON model objects
	 *  will still be generated as applicable.
	 */
	public static <AnimationEntity extends AEntityC_Definable<?>> List<RenderableModelObject<AnimationEntity>> generateRenderables(String modelLocation){
		Map<String, Float[][]> parsedModelObjects = parseModel(modelLocation);
		List<RenderableModelObject<AnimationEntity>> modelObjects = new ArrayList<RenderableModelObject<AnimationEntity>>();
		for(String parsedObjectName : parsedModelObjects.keySet()){
			//If we are a window, adjust our UV mapping points to map to a single texture.
			//Add these adjusted points, and then invert them for the interior component.
			if(parsedObjectName.toLowerCase().contains(WINDOW_OBJECT_NAME)){
				Float[][] parsedObject = parsedModelObjects.get(parsedObjectName);
				normalizeUVs(parsedObject);
				
				Float[][] invertedObject = new Float[parsedObject.length][8];
				for(int i=0, j=parsedObject.length-1; i<parsedObject.length; ++i, --j){
					invertedObject[j] = parsedObject[i];
				}
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName + INTERIOR_WINDOW_SUFFIX, modelObjects, invertedObject));
			}
			
			//If we are a tread roller, make a roller rather than a standard object.
			if(parsedObjectName.toLowerCase().contains(ROLLER_OBJECT_NAME)){
				modelObjects.add(new RenderableTreadRoller<AnimationEntity>(modelLocation, parsedObjectName, modelObjects, parsedModelObjects.get(parsedObjectName)));
			}else{
				modelObjects.add(new RenderableModelObject<AnimationEntity>(modelLocation, parsedObjectName, modelObjects, parsedModelObjects.get(parsedObjectName)));
			}
		}
		return modelObjects;
	}

	/**
	 *  Helper method to normalize a set of UVs for rendering.
	 */
	public static void normalizeUVs(Float[][] parsedObject){
		for(int i=0; i<parsedObject.length; ++i){
			if(parsedObject.length > 3 && i%6 >= 3){
				//Second-half of a quad.
				switch(i%6){
					case(3): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(5): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
				}
			}else{
				//Normal tri or first half of quad using tri mapping.
				switch(i%6){
					case(0): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
					case(1): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 1.0F; break;
					case(2): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					
					case(3): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 1.0F; break;
					case(4): parsedObject[i][3] = 1.0F; parsedObject[i][4] = 0.0F; break;
					case(5): parsedObject[i][3] = 0.0F; parsedObject[i][4] = 0.0F; break;
				}
			}
		}
	}
}
