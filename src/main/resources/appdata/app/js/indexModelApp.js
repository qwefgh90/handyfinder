define(['angular'], function(angular){
	var app = angular.module('indexModelApp', []);
	app.factory("SearchModel",['$rootScope','$http', '$q', '$log', 'Document', function($rootScope, $http, $q, $log, Document){
		var service = {
				model : {
					searchFlag : false,
					searchKeyword : '',
					searchResult : [],
					searchTime : 0,
					searchTryCount : 0
				},
				
				setSearchFlag: function(b){
					this.searchFlag = b;
				},
				
				lazyLoadDocumentContent: function(document, keyword){
					var pathString = document.pathString;
					var deferred = $q.defer();

					// ajax $http
					var headers = {
							'Accept' : 'application/json'
					};
					var params = {
							keyword : keyword,
							pathString : pathString
					};
					var config = {
							params : params,
							headers : headers
					};

					$http.get(url = '/document/content', config).then(function(response) {
						if (response.status == 200) {
							var content = response.data;
							document.contents = content;
							$log.log('path : '+ document.pathString + ", content : " + content);
							deferred.resolve(response.data);
						} else {
							$log.log('getDocumentContent failed');
							deferred.reject(response.data);
						}
					}, function(response) {
						$log.log('getDocumentContent failed');
						deferred.reject(response.data);
					}, function(response) {
						$log.log('getDocumentContent noti');
						deferred.reject(response.data);
					});
					return deferred.promise;
				},

				search : function(keyword){
					var deferred = $q.defer();

					// ajax $http
					var headers = {
							'Accept' : 'application/json'
					};
					var params = {
							keyword : keyword
					};
					var config = {
							params : params,
							headers : headers
					};

					var milliseconds = (new Date).getTime();
					if(this.model.searchFlag == true)
						return;
					if(keyword.length == 0){
						return;
					}
					this.model.searchFlag = true;
					this.model.searchTryCount = this.model.searchTryCount + 1;
					var self = this;	//http://stackoverflow.com/questions/20279484/how-to-access-the-correct-this-context-inside-a-callback

					$http.get(url = '/documents', config).then(function(response) {
						if (response.status == 200) {
							var json = response.data;
							var toMiliseconds = (new Date).getTime();
							self.model.searchResult = [];
							for(var i = 0 ; i < json.length ; i ++){
								var data = json[i];
								var document = new Document(data.createdTime, data.modifiedTime, data.title, data.pathString
										, data.contents, data.parentPathString, data.fileSize, data.mimeType, data.exist);
								self.model.searchResult.push(document);
								self.lazyLoadDocumentContent(document, keyword);
							}
							self.model.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000
							self.model.searchFlag = false;
							$log.log('search success');
							deferred.resolve(response.data);
						} else {
							deferred.reject(response.data);
						}
					}, function(response) {
						var toMiliseconds = (new Date).getTime();
						self.model.searchFlag = false;
						self.model.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000
						$log.log('search failed');
						deferred.reject(response.data);
					}, function(response) {
						var toMiliseconds = (new Date).getTime();
						self.model.searchFlag = false;
						self.model.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000
						$log.log('search noti');
						deferred.reject(response.data);
					});
					return deferred.promise;
				},

				SaveState: function () {
					sessionStorage.userService = angular.toJson(service.model);
				},

				RestoreState: function () {
					service.model = angular.fromJson(sessionStorage.userService);
				}

				// $rootScope.$on("savestate", service.SaveState);
				// $rootScope.$on("restorestate", service.RestoreState);

		};
		return service;
	}]);


	app.factory("OptionModel",['$rootScope', 'Option', '$q', '$log', '$http', 'SearchModel', function($rootScope, Option, $q, $log, $http, SearchModel){
		var option = new Option();
		var service ={
				model : {
					option : option
				},
				getOptions: function() {
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
					$http.get(url = '/options', config).then(function(response) {
						if (response.status == 200) {
							var msg = response.data;
							self.model.option.limitCountOfResult = msg.limitCountOfResult;
							self.model.option.maximumDocumentMBSize = msg.maximumDocumentMBSize;
							self.model.option.keywordMode = msg.keywordMode;
							$log.log('option loaded ' + self.model.option.limitCountOfResult + ', ' + self.model.option.maximumDocumentMBSize
									+ ', ' + self.model.option.keywordMode);
							deferred.resolve(response.data);
						} else {
							deferred.reject('getDirectories() fail');
						}
					}, function(response) {
						$log.log('option fail to load');
						deferred.reject(response.data);
					}, function(response) {
						$log.log('option noti');
						deferred.reject(response.data);
					});
					return deferred.promise;
					// then(successCallback, errorCallback, notifyCallback)
				},
				
				updateOptions: function() {
					var deferred = $q.defer();

					SearchModel.setSearchFlag(true);
					
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
					var data = JSON.stringify(this.model.option);

					$http.post(url = '/options', data, config).then(function(response) {
						if (response.status == 200) {
							SearchModel.setSearchFlag(false);
							$log.log('finish to save option.');
							deferred.resolve();
						} else {
							SearchModel.setSearchFlag(false);
							$log.log('fail to save option.');
							deferred.reject();
						}
					}, function(response) {
						SearchModel.setSearchFlag(false);
						$log.log('fail to save option.');
						deferred.reject();
					}, function(response) {
						SearchModel.setSearchFlag(false);
						$log.log('fail to save option.');
						deferred.reject();
					});
					return deferred.promise;
				}

		};
		return service;
	}]);

	app.constant('INIT_COUNT', 100);
	app.constant('LOAD_MORE_COUNT', 500);
	app.factory("SupportTypeUI",['$q', '$http', '$timeout', '$log', 'LOAD_MORE_COUNT', 'INIT_COUNT', function($q, $http, $timeout, $log, LOAD_MORE_COUNT, INIT_COUNT){
		var SupportTypeUI = function(indexModel){
			this.searchedTypeKeyword = '';
			this.totalDisplayed = INIT_COUNT;
			this.searchedType = '';
			this.enterHitCount = 0;
			this.indexModel = indexModel;
		}
		
		SupportTypeUI.prototype.changeSearchKeyword = function(searchedTypeKeyword){
			this.enterHitCount = 0;
			if(searchedTypeKeyword == '')
				return;
			for(var i = 0; i < this.totalDisplayed; i++){
				if(this.indexModel.supportTypes[i].type.indexOf(searchedTypeKeyword) > -1){
					this.searchedType = this.indexModel.supportTypes[i].type;
					$log.log(this.indexModel.supportTypes[i].type + ' is matched');
					$log.log('0 hit');
					return;

				}
			}
		};
		

		SupportTypeUI.prototype.nextSearch = function(searchedTypeKeyword){
			this.enterHitCount += 1;
			var searchStack = [];
			var offsetCounter = 0;
			var searchedType = '';
			if(searchedTypeKeyword == '')
				return;
			for(var i = 0; i < this.totalDisplayed; i++){
				//matched
				if(this.indexModel.supportTypes[i].type.indexOf(searchedTypeKeyword) > -1){
					searchedType = this.indexModel.supportTypes[i].type;
					searchStack.push(searchedType);
				}
			}
			//enter overflow
			if(searchStack.length <= this.enterHitCount){
				this.searchedType = searchStack[0]
				this.enterHitCount = 0;
				$log.log('enter hit overflow');
				$log.log(this.enterHitCount + ' offset hit');
			}else{
				this.searchedType = searchStack[this.enterHitCount];
				$log.log(this.enterHitCount + ' offset hit');
			}
		}
		SupportTypeUI.prototype.loadMore = function(){
			this.totalDisplayed = this.totalDisplayed + LOAD_MORE_COUNT;
			if(this.indexModel.supportTypes.length < this.totalDisplayed){
				this.totalDisplayed = this.indexModel.supportTypes.length;
			}
		}
		
		return SupportTypeUI;
	}]);
	
	app.factory("IndexModel",['$rootScope', '$q', '$http', '$log', '$timeout', function($rootScope, $q, $http, $log, $timeout){
		var service = {
				model : {
					searchedType : '',
					indexDocumentCount : 0,
					auto_update_index : false,
					auto_clean_files : false,
					supportTypes : [], //{type:xx, used:xx}
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
								$log.log('supportTypes loaded : ' + msg.length);
								deferred.resolve(response.data);
							} else {
								$log.log('supportTypes fail to load');
								deferred.reject('supportTypes() fail');
							}
						}, function(response) {
							$log.log('supportTypes fail to load');
							deferred.reject(response.data);
						}, function(response) {
							$log.log('supportTypes fail to load');
							deferred.reject(response.data);
						});
					}else{
						$log.log('already supportTypes loaded');
						$timeout(function(){deferred.notify('already supportTypes loaded!!!');}, 100);
					}
					return deferred.promise;
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

	app.factory("Option", function() {
		// Define the constructor function.
		/*
		 * 
	private int limitCountOfResult;
	private int maximumDocumentMBSize;
		 * 
		 */
		function Option(limitCountOfResult, maximumDocumentMBSize, keywordMode) {
			this.limitCountOfResult = limitCountOfResult;
			this.maximumDocumentMBSize = maximumDocumentMBSize;
			this.keywordMode = keywordMode;
		}
		function Option() {
			this.limitCountOfResult = -1;
			this.maximumDocumentMBSize = -1;
			this.keywordMode = 'OR';
		}

		// Return constructor - this is what defines the actual
		// injectable in the DI framework.
		return (Option);
	}); 

	app.factory("Path", function() {
		// Define the constructor function.
		function Path(path, used, recursively) {
			this.pathString = path;
			this.used = used;
			this.recursively = recursively;
		}

		// Define the "instance" methods using the prototype
		// and standard prototypal inheritance.
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
		// Define the "class" / "static" methods. These are
		// utility methods on the class itself; they do not
		// have access to the "this" reference.
		Path.createInstance = function(path) {
			return new Path(path, true, true);
		};
		// Return constructor - this is what defines the actual
		// injectable in the DI framework.
		return (Path);
	}); 
	app.factory("Document", function() {
		// Define the constructor function.
		function Document(createdTime, modifiedTime, title, pathString, contents, parentPathString, fileSize, mimeType, exist) {
			/*
			 * private long createdTime; private long modifiedTime; private String
			 * title; private String pathString; private String contents;
			 */
			this.createdTime = createdTime;
			this.modifiedTime = modifiedTime;
			this.title = title;
			this.pathString = pathString;
			this.contents = contents;
			this.parentPathString = parentPathString;
			this.fileSize = fileSize;
			this.mimeType = mimeType;
			this.exist = exist;
		}

		// Define the "instance" methods using the prototype
		// and standard prototypal inheritance.
		Document.prototype = {
		};
		// Define the "class" / "static" methods. These are
		// utility methods on the class itself; they do not
		// have access to the "this" reference.
		// Path.createInstance = function(path) {
		// return new Path(path, true, true);
		// };
		// Return constructor - this is what defines the actual
		// injectable in the DI framework.
		return (Document);
	}); 
	return app;
});