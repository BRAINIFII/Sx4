package com.sx4.utils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class GiveawayUtils {
	
	private static Random random = new Random();
		
	@SafeVarargs
	public static <Type> Set<Type> getRandomSample(int amount, Type... array) {
		return getRandomSample(List.of(array), amount);
	}
	
	public static <Type> Set<Type> getRandomSample(List<Type> list, int amount) {
		Set<Type> returnSet = new HashSet<>();

		while (returnSet.size() != amount) {
			returnSet.add(list.get(random.nextInt(list.size())));
		}
			
		return returnSet;
	}
	
	public static <Type> List<Type> getRandomSampleAndRemove(List<Type> list, int amount) {
		List<Type> returnList = new ArrayList<>();

		while (returnList.size() != amount) {
			Type item = list.get(random.nextInt(list.size()));
			list.remove(item);
			returnList.add(item);
		}
			
		return returnList;
	}
	
}
