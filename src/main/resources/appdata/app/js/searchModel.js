define(['angular'], function(angular){
	var app = angular.module('SearchModel', []);
	app.factory("SearchModel", ['$rootScope','$http', '$q', '$log', 'Document', function($rootScope, $http, $q, $log, Document){
		var service = {
				model : {
					searchFlag : false,
					searchKeyword : '',
					searchResult : [],
					searchTime : 0,
					searchTryCount : 0,
					page : 1
				},

				setSearchFlag: function(b){
					this.searchFlag = b;
				},

				lazyLoadDocumentContent: function(document){
					var pathString = document.pathString;
					var keyword = document.keyword;
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
					this.model.page = 1;
					
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
										, data.contents, data.parentPathString, data.fileSize, data.mimeType, data.exist, keyword);
								self.model.searchResult.push(document);
								//self.lazyLoadDocumentContent(document, keyword);
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
				
				/*
				SaveState: function () {
					sessionStorage.userService = angular.toJson(service.model);
				},

				RestoreState: function () {
					service.model = angular.fromJson(sessionStorage.userService);
				}

				$rootScope.$on("savestate", service.SaveState);
				$rootScope.$on("restorestate", service.RestoreState);
				*/
		};
		return service;
	}]);

	app.factory("Document", function() {
		// Define the constructor function.
		function Document(createdTime, modifiedTime, title, pathString, contents, parentPathString, fileSize, mimeType, exist, keyword) {
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
			this.loaded = false;
			this.keyword = keyword;
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