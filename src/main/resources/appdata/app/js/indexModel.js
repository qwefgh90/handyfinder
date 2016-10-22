define(['angular', 'webSocketModel'], function(angular){
	var app = angular.module('IndexModel', ['WebSocketModel']);
	app.factory("IndexModel",['$rootScope', '$q', '$http', '$log', '$timeout', '$interval', 'ProgressService', function($rootScope, $q, $http, $log, $timeout, $interval, progressService){
		var service = {
				model : {
					searchedType : '',
					indexDocumentCount : 0,
					auto_update_index : false,
					auto_clean_files : false,
					//supportTypes : [], //{type:xx, used:xx}
					pathList : [],
					select_toggle : false,
					index_manager_status : {
						open : true
					},
					index_option_status : {
						open : true
					},
					index_progress_status : {
						open : false,
						progress : false,
						progressItemCount : 0,
						progressBarVisible : false,
						alerts : [{// 0
							open : false,
							type : 'success',
							msg : 'Directories are stored on disk!',
							timeout : 10000
						}, {// 1
							open : false,
							type : 'danger',
							msg : 'Storing directories is failed on disk!',
							timeout : 10000
						}, {// 2
							open : false,
							type : 'success',
							msg : 'Directories are loaded on disk!',
							timeout : 10000
						}, {// 3
							open : false,
							type : 'danger',
							msg : 'Loading directories is failed from disk!',
							timeout : 10000
						}, {// 4
							open : false,
							type : 'success',
							msg : 'Ready to search your files!',
							timeout : 10000
						}, {// 5
							open : false,
							type : 'success',
							msg : 'Ready to search your files!',
							timeout : 10000
						}],
						alertQ : [],
						setMsgAlertQ : function(index, msg){
							if(msg == undefined){
								return;
							}
							if(index < 0 && index >= this.alerts.length){
								return;
							}
							this.alerts[index].msg = msg;
						},
						addAlertQ : function(index) {
							if (this.alertQ.indexOf(this.alerts[index]) != -1)	//already added
								return;
							this.alertQ.push(this.alerts[index]);
							this.progressItemCount++;
							this.refreshState();
						},
						removeAlertQ : function(queueSeq) {
							if (this.alertQ.length <= queueSeq)	//invalid request
								return;
							this.alertQ.splice(queueSeq, 1);
							this.progressItemCount--;
							this.refreshState();
						},
						refreshState : function() {
							if (this.progressItemCount > 0 || this.progressBarVisible == true) {
								this.progress = true;
								this.open = true;
							} else {
								this.progress = false;
								this.open = false;
							}
						}
					},
					processIndex : 0,
					totalProcessCount : 100,
					processPath : '',
					state : 'TERMINATE', //START PROGRESS TERMINATE
					updateSummary : {
						countOfDeleted : 0,
						countOfExcluded : 0,
						countOfModified : 0,
						state : 'TERMINATE' //START TERMINATE
					},
					intervalStopObject : undefined,
					intervalTurn : 0,
					intervalTime : 10000,
					running : 'READY', //READY RUNNING
					connect : function(){
						return progressService.connect();
					},
					indexControllerSubscribed : false,
					run : function(){	//schedule to index 
						var self = this;
						self.running = 'RUNNING'
							progressService.sendStartIndex();
						self.intervalStopObject = $interval(function(){
							if(self.intervalTurn % 2 == 0){
								$log.debug('try update index...');
								progressService.sendUpdateIndex();
							}else{
								$log.debug('try start index...');
								progressService.sendStartIndex();
							}

							self.intervalTurn = self.intervalTurn + 1;
							if(self.intervalTurn == 100)
								self.intervalTurn = 0;
						}, self.intervalTime);
					},
					stop : function(){	//stop to index
						var self = this;
						if(self.intervalStopObject == undefined){
							$log.info('not started yet');
							return;
						}
						if(self.running != 'RUNNING'){
							$log.error('illegal state. not running');
							return;
						}
						$log.info('stopping index...');
						$interval.cancel(self.intervalStopObject);
						self.intervalStopObject = undefined;
						self.running = 'READY';		// change state
						progressService.sendStopIndex();
					}
				},

				SaveState: function () {
					sessionStorage.userService = angular.toJson(service.model);
				},

				RestoreState: function () {
					service.model = angular.fromJson(sessionStorage.userService);
				},

				getDocumentCount: function() {
					var deferred = $q.defer();

					// ajax $http
					var headers = {
							'Accept' : 'application/json',
							'Content-Type' : 'application/json'
					};
					var params = {
					};
					var config = {
							'params' : params,
							'headers' : headers
					};

					var self = this;
					$http.get(url = '/documents/count', config).then(function(response) {
						if (response.status == 200) {
							self.model.indexDocumentCount = response.data
							deferred.resolve(response.data);
						} else {
							deferred.reject('getDocumentCount() fail');
						}
					}, function(response) {
						deferred.reject(response.data);
					}, function(response) {
						deferred.reject(response.data);
					});
					return deferred.promise;
					// then(successCallback, errorCallback, notifyCallback)
				},
				getDirectories: function() {
					var deferred = $q.defer();

					// ajax $http
					var headers = {
							'Accept' : 'application/json',
							'Content-Type' : 'application/json'
					};
					var params = {
					};
					var config = {
							'params' : params,
							'headers' : headers
					};

					$http.get(url = '/directories', config).then(function(response) {
						if (response.status == 200) {
							deferred.resolve(response.data);
						} else {
							deferred.reject('getDirectories() fail');
						}
					}, function(response) {
						deferred.reject(response.data);
					}, function(response) {
						deferred.reject(response.data);
					});
					return deferred.promise;
					// then(successCallback, errorCallback, notifyCallback)
				},

				updateDirectories : function() {
					var deferred = $q.defer();

					// ajax $http
					var headers = {
							'Accept' : 'application/json',
							'Content-Type' : 'application/json'
					};
					var params = {
					};
					var config = {
							'headers' : headers
					};
					var data = JSON.stringify(this.model.pathList);

					$http.post(url = '/directories', data, config).then(function(response) {
						if (response.status == 200) {
							deferred.resolve();
						} else {
							deferred.reject();
						}
					}, function(response) {
						deferred.reject();
					}, function(response) {
						deferred.reject();
					});
					return deferred.promise;
				}
		};
		
		return service;

	}]);

	app.factory("Path", function() {
		// Define the constructor function.
		function Path(path, used, recursively) {
			this.pathString = path;
			this.used = used;
			this.recursively = recursively;
		}

		Path.prototype = {
				getPathString : function() {
					return (this.pathString);
				},
				getUsed : function() {
					return (this.used);
				},
				getRecursively : function() {
					return (this.recursively);
				}
		};
		
		Path.createInstance = function(path) {
			return new Path(path, true, true);
		};
		
		return (Path);
	}); 
	return app;
});