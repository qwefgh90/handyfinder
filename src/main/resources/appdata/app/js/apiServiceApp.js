define(['angular'], function(angular){
	var app = angular.module('apiServiceApp',[]);

	app.factory('apiService', ['$http', '$q', '$log', function($http, $q, $log){
	
		var getDocumentCount = function() {
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
		};


		return {
			getDocumentCount : getDocumentCount};
	}]);
	return app;
});
