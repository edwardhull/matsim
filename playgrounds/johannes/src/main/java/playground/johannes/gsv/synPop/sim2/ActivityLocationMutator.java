/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.johannes.gsv.synPop.sim2;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.matsim.api.core.v01.Coord;
import org.matsim.core.api.experimental.facilities.ActivityFacilities;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityOption;

import playground.johannes.gsv.misc.QuadTree;
import playground.johannes.gsv.synPop.CommonKeys;
import playground.johannes.gsv.synPop.ProxyObject;
import playground.johannes.gsv.synPop.ProxyPerson;
import playground.johannes.gsv.synPop.mid.MIDKeys;
import playground.johannes.gsv.synPop.sim.MutateActivityLocation;
import playground.johannes.gsv.synPop.sim.RandomFacilities;

/**
 * @author johannes
 *
 */
public class ActivityLocationMutator implements Mutator {

private final String blacklist;
	
	private final Random random;
	
	private final RandomFacilities rndFacilities;
	
	private final ActivityFacilities facilities;
	
	private final Map<String, QuadTree<ActivityFacility>> quadTrees;
	
	private final double mutationRange = 2000;
	
	private ProxyObject currentAct;
	
	private ActivityFacility currentFacility;
	
	public ActivityLocationMutator(ActivityFacilities facilities, Random random, String type) {
		this.random = random;
		this.blacklist = type;
		this.facilities = facilities;
		this.rndFacilities = new RandomFacilities(facilities, random);
		
		double minx = Double.MAX_VALUE;
		double miny = Double.MAX_VALUE;
		double maxx = 0;
		double maxy = 0;
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			Coord coord = facility.getCoord();
			minx = Math.min(minx, coord.getX());
			miny = Math.min(miny, coord.getY());
			maxx = Math.max(maxx, coord.getX());
			maxy = Math.max(maxy, coord.getY());
		}
		
		quadTrees = new HashMap<String, QuadTree<ActivityFacility>>();
		for(ActivityFacility facility : facilities.getFacilities().values()) {
			for(ActivityOption option : facility.getActivityOptions().values()) {
				QuadTree<ActivityFacility> quadTree = quadTrees.get(option.getType());
				if(quadTree == null) {
					quadTree = new QuadTree<ActivityFacility>(minx, miny, maxx, maxy);
					quadTrees.put(option.getType(), quadTree);
				}
				Coord coord = facility.getCoord();
				quadTree.put(coord.getX(), coord.getY(), facility);
			}
		}
	}
	
	@Override
	public boolean modify(ProxyPerson person) {
		List<ProxyObject> activities = person.getPlan().getActivities();
		
		int idx = random.nextInt(activities.size());
		ProxyObject act = activities.get(idx);
		String type = (String) act.getAttribute(CommonKeys.ACTIVITY_TYPE);
		
		if(blacklist == null || !blacklist.equalsIgnoreCase(type)) {
			currentAct = act;
			currentFacility = (ActivityFacility) act.getUserData(MutateActivityLocation.USER_DATA_KEY);
			
			ActivityFacility facility = null;
//			if(idx > 0) {
//				ProxyObject leg = person.getPlan().getLegs().get(idx - 1);
//				String value = leg.getAttribute(MIDKeys.LEG_DISTANCE);
//				if(value != null) {
//					ProxyObject prev = activities.get(idx - 1);
//					ActivityFacility fac = (ActivityFacility) prev.getUserData(MutateActivityLocation.USER_DATA_KEY);
//					if(fac == null) {
//						fac = facilities.getFacilities().get(new IdImpl(prev.getAttribute(CommonKeys.ACTIVITY_FACILITY)));
//						prev.setUserData(MutateActivityLocation.USER_DATA_KEY, fac);
//					}
//					double dist = Double.parseDouble(value);
//					QuadTree<ActivityFacility> quadTree = quadTrees.get(act.getAttribute(CommonKeys.ACTIVITY_TYPE));
//					List<ActivityFacility> list = new ArrayList<ActivityFacility>(quadTree.get(fac.getCoord().getX(), fac.getCoord().getY(), dist - mutationRange, dist + mutationRange));
//					
//					facility = list.get(random.nextInt(list.size()));
//				} else {
//					facility = rndFacilities.randomFacility(type);
//				}
//			} else {
				facility = rndFacilities.randomFacility(type);
//			}
			act.setAttribute(CommonKeys.ACTIVITY_FACILITY, facility.getId().toString());
			act.setUserData(MutateActivityLocation.USER_DATA_KEY, facility);
			return true;
		} else {
			return false;
		}

	}

	/* (non-Javadoc)
	 * @see playground.johannes.gsv.synPop.sim2.Mutator#revert(playground.johannes.gsv.synPop.ProxyPerson)
	 */
	@Override
	public void revert(ProxyPerson person) {
		currentAct.setAttribute(CommonKeys.ACTIVITY_FACILITY, currentFacility.getId().toString());
		currentAct.setUserData(MutateActivityLocation.USER_DATA_KEY, currentFacility);
		
		currentAct = null;
		currentFacility = null;

	}

}