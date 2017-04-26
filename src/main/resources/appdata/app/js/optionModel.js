define(['angular', 'searchModel'], function(angular){
	var app = angular.module('OptionModel', ['SearchModel']);
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
							self.model.option.firstStart = msg.firstStart
							self.model.option.pathTarget = msg.pathTarget;
							self.model.option.contentTarget = msg.contentTarget;
							self.model.option.diskUseLimit = msg.diskUseLimit;
							$log.debug('option loaded ' + self.model);
							deferred.resolve(response.data);
						} else {
							deferred.reject('getDirectories() fail');
						}
					}, function(response) {
						$log.log('option fail to load');
						deferred.reject(response.data);
					}, function(response) {
						$log.log('option notified');
						deferred.reject(response.data);
					});
					return deferred.promise;
				},

				updateOptions: function() {
					SearchModel.setSearchFlag(true);
					
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

	app.factory("SupportTypeModel", ['$q', '$http', '$log', '$timeout', function($q, $http, $log, $timeout){
		var service = {
				model: {
					supportTypes : [] //{type:xx, used:xx}},
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
				}
		}
		
		//initialize model data loaded from server
		var typePromise = service.getSupportTypes();
		typePromise.then(function(msg) {
			$log.debug('supportTypes loaded : ' + msg.length);
		}, function(msg) {
			$log.error('supportTypes fail to load');
		}, function(msg) {
			$log.debug('supportTypes notify');
		});

		return service;
	}]);

	app.constant('INIT_COUNT', 100);
	app.constant('LOAD_MORE_COUNT', 500);
	app.factory("SupportTypeUI",['$q', '$http', '$timeout', '$log', 'SupportTypeModel', 'LOAD_MORE_COUNT', 'INIT_COUNT', function($q, $http, $timeout, $log, SupportTypeModel, LOAD_MORE_COUNT, INIT_COUNT){
		var SupportTypeUI = function(SupportTypeModel){
			this.searchedTypeKeyword = '';
			this.totalDisplayed = INIT_COUNT;
			this.searchedType = '';
			this.enterHitCount = 0;
			this.indexModel = SupportTypeModel.model;
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


	app.factory("Option", function() {
		// Define the constructor function.
		function Option() {
			this.limitCountOfResult = -1;
			this.maximumDocumentMBSize = -1;
			this.keywordMode = 'OR';
			this.firstStart = false;
			this.pathTarget = true;
			this.contentTarget = true;
			this.diskUseLimit = 0;
		}

		// Return constructor - this is what defines the actual
		// injectable in the DI framework.
		return (Option);
	}); 

	return app;

});