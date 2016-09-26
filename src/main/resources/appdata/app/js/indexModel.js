define(['angular'], function(angular){
	var app = angular.module('IndexModel', []);
	app.factory("IndexModel",['$rootScope', '$q', '$http', '$log', '$timeout', function($rootScope, $q, $http, $log, $timeout){
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
					running : 'READY' //READY RUNNING WAITING (ONLY FORM RUNNING TO WAITING STATE IN SCRIPT)
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

					$http.get(url = '/documents/count', config).then(function(response) {
						if (response.status == 200) {
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
				/*
				getSupportTypes: function() {
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

					if(this.model.supportTypes.length == 0){
						$http.get(url = '/supportTypes', config).then(function(response) {
							if (response.status == 200) {
								var msg = response.data;
								self.model.supportTypes = msg;
								deferred.resolve(response.data);
							} else {
								deferred.reject('supportTypes() fail');
							}
						}, function(response) {
							deferred.reject(response.data);
						}, function(response) {
							deferred.reject(response.data);
						});
					}else{
						$log.log('already supportTypes loaded');
						$timeout(function(){deferred.notify('already supportTypes loaded!!!');}, 100);
					}
					return deferred.promise;
				},
				updateSupportType: function(supportTypeDto) {
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
					var data = JSON.stringify(supportTypeDto);

					$http.post(url = '/supportType', data, config).then(function(response) {
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
					// then(successCallback, errorCallback, notifyCallback)
				},
				
				updateSupportTypeList: function() {
					var list = this.model.supportTypes;
					
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
					var data = JSON.stringify(list);

					$http.post(url = '/supportTypes', data, config).then(function(response) {
						if (response.status == 200) {
							$log.log('successful update support list');
							deferred.resolve();
						} else {
							$log.log('supportType fail to update ');
							deferred.reject();
						}
					}, function(response) {
						$log.log('supportType fail to update ');
						deferred.reject();
					}, function(response) {
						$log.log('supportType fail to update ');
						deferred.reject();
					});
					return deferred.promise;
					// then(successCallback, errorCallback, notifyCallback)
				},
				*/
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
					// then(successCallback, errorCallback, notifyCallback)
				}
	
				// $rootScope.$on("savestate", service.SaveState);
				// $rootScope.$on("restorestate", service.RestoreState);
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