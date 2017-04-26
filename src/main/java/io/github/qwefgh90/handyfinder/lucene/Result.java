package io.github.qwefgh90.handyfinder.lucene;

import java.nio.file.Path;
import java.util.Optional;

public class Result {
	public static class IndexResult{
		public enum IndexResultCode{
			SUCCESS, STOPPED, DISK_IS_FULL, EXCEPTION;
		}
		public final IndexResultCode code;
		public final Optional<String> msg;
		public final Optional<Path> path;
		private IndexResult(IndexResultCode code, Optional<String> msg, Optional<Path> path){
			this.code = code;
			this.msg = msg;
			this.path = path;
		}
		public static final IndexResult SUCCESS = new IndexResult(IndexResultCode.SUCCESS,Optional.empty(),Optional.empty());;
		public static final IndexResult STOPPED = new IndexResult(IndexResultCode.SUCCESS,Optional.empty(),Optional.empty());;
		public static final IndexResult DISK_IS_FULL = new IndexResult(IndexResultCode.SUCCESS,Optional.empty(),Optional.empty());;
		public static final IndexResult EXCEPTION = new IndexResult(IndexResultCode.SUCCESS,Optional.empty(),Optional.empty());;

		public static IndexResult EXCEPTION(Optional<String> msg, Optional<Path> path){
			return new IndexResult(IndexResultCode.EXCEPTION, msg, path);
		}
	}
}
