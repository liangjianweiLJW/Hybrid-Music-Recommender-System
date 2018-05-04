package top.wangruns.trackstacking.algorithm;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

import top.wangruns.trackstacking.model.Collection;
import top.wangruns.trackstacking.model.DownloadRecord;
import top.wangruns.trackstacking.model.PlayRecord;
import top.wangruns.trackstacking.model.User;

public class DataTranslate {
	private final static float PLAY_SCORE=1f;
	private final static float DOWNLOAD_SCORE=2f;
	private final static float COLLECTION_SCORE=5f;
	private final static float MAX_SCORE=10f;

	/**
	 * 构建用户频率矩阵来近似用户评分，对于某些系统而言，我们是不可能获取到用户对某些项目的评分的，但是我们可以利用用户的
	 * 行为习惯来反映用户的“评分”，比如一个用户常常收听某一首歌，那么我们可以推断该用户喜欢该歌曲的可能性很大.
	 * 总分10分，主动播放一次1分，下载2分，收藏5分，如果超过10分，按10分计算.
	 * @param userIdList 
	 * 用户Id列表
	 * @param songIdList 
	 * 歌曲Id列表
	 * @param downloadList
	 * 用户的下载记录列表
	 * @param playList
	 * 用户的播放记录列表
	 * @param collectionList
	 * 用户的收藏记录列表
	 * @return
	 * 用户Id-歌曲Id 频率矩阵
	 */
	public static Map<Integer, Float[]> getFrequencyMatrix(List<Integer> userIdList, final List<Integer> songIdList,
			List<DownloadRecord> downloadList, List<PlayRecord> playList, List<Collection> collectionList) {
		// TODO Auto-generated method stub
		final Map<Integer,Float[]> user2songRatingMatrix=new HashMap<Integer, Float[]>();
		final int songLen=songIdList.size();
		//获取用户-歌曲 下载映射
		final Map<Integer,Integer[]> userId2songIdDownloadMap=getUserId2songIdRecordMap(downloadList,false);
		//获取用户-歌曲 收藏映射
		final Map<Integer,Integer[]> userId2songIdCollectionMap=getUserId2songIdRecordMap(collectionList,false);
		//获取用户-歌曲-次数 播放映射
		final Map<Integer,Integer[]> userId2songIdPlayMap=getUserId2songIdRecordMap(playList,true);
		
		userIdList.forEach(new Consumer<Integer>() {

			public void accept(Integer userId) {
				// TODO Auto-generated method stub
				Float[] curUserRatingArray=new Float[songLen];
				int songIndex=0;
				//处理每一首歌曲
				for(Integer songId:songIdList) {
					/**
					 * 处理下载，这里不考虑下载次数
					 */
					if(userId2songIdDownloadMap.get(userId)!=null && userId2songIdDownloadMap.get(userId)[0]==songId) {
						//当前用户下载过的歌曲
						curUserRatingArray[songIndex]+=DOWNLOAD_SCORE;
					}
					
					/**
					 * 处理收藏，这里没有次数
					 */
					if(userId2songIdCollectionMap.get(userId)!=null && userId2songIdCollectionMap.get(userId)[0]==songId) {
						//当前用户收藏的歌曲
						curUserRatingArray[songIndex]+=COLLECTION_SCORE;
					}
					
					/**
					 * 处理播放，考虑播放次数
					 */
					if(userId2songIdPlayMap.get(userId)!=null && userId2songIdPlayMap.get(userId)[0]==songId) {
						//当前用户播放过的歌曲
						int count=userId2songIdPlayMap.get(userId)[1];
						curUserRatingArray[songIndex]+=PLAY_SCORE + count;
					}
					
					/**
					 * 处理最大得分，超过最大得分，记为最大得分
					 */
					if(curUserRatingArray[songIndex]>MAX_SCORE) {
						curUserRatingArray[songIndex]=MAX_SCORE;
					}
					//处理下一首歌
					songIndex++;
				}
				//处理完一个用户
				user2songRatingMatrix.put(userId, curUserRatingArray);
			}
			
		});
		return user2songRatingMatrix;
	}

	/**
	 * 获取用户Id - 歌曲Id 的映射Map
	 * @param recordList
	 * 包含userId，songId的记录列表
	 * @param isCount
	 * 是否需要计数。如果true，则Integer[1]存放计数。
	 * @return
	 * Map<songId,[songId,count]>
	 */
	private static <T> Map<Integer, Integer[]> getUserId2songIdRecordMap(List<T> recordList,final boolean isCount) {
		// TODO Auto-generated method stub
		final Map<Integer,Integer[]> userId2songIdRecordMap=new HashMap<Integer, Integer[]>();
		
		recordList.forEach(new Consumer<T>() {

			public void accept(T t) {
				// TODO Auto-generated method stub
				try {
					//利用反射获和泛型获取不同类型表的相同属性
					Field userIdField=t.getClass().getField("userId");
					Field songIdField=t.getClass().getField("songId");
					userIdField.setAccessible(true);
					songIdField.setAccessible(true);
					int userId=userIdField.getInt(t);
					int songId=userIdField.getInt(t);
					Integer songId_count[]=new Integer[2];
					if(!isCount) {
						//不需要计数
						if(!userId2songIdRecordMap.containsKey(userId)) {
							songId_count[0]=songId;
							userId2songIdRecordMap.put(userId, songId_count);
						}
					}else {
						//需要计数
						if(!userId2songIdRecordMap.containsKey(userId)) {
							//第一次出现,计数为1
							songId_count[0]=songId;
							songId_count[1]=1;
							userId2songIdRecordMap.put(userId, songId_count);
						}else {
							//非第一次出现,计数+1
							songId_count=userId2songIdRecordMap.get(userId);
							songId_count[1]++;
							userId2songIdRecordMap.put(userId, songId_count);
						}
					}
					
				}catch (NoSuchFieldException e) {
					e.printStackTrace();
				} catch (IllegalArgumentException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
			}
			
		});
		return userId2songIdRecordMap;
	}

//	private static Map<Integer, Integer>getUserId2songIdDownloadMap(List<DownloadRecord> downloadList) {
//		// TODO Auto-generated method stub
//		final Map<Integer,Integer> userId2songIdDownloadMap=new HashMap<Integer, Integer>();
//		downloadList.forEach(new Consumer<DownloadRecord>() {
//
//			public void accept(DownloadRecord t) {
//				// TODO Auto-generated method stub
//				if(!userId2songIdDownloadMap.containsKey(t.getUserId())) {
//					userId2songIdDownloadMap.put(t.getUserId(), t.getSongId());
//				}
//			}
//			
//		});
//		return userId2songIdDownloadMap;
//	}
	

}
