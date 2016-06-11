var app = angular.module('apiServiceApp',[]);


app.factory('apiService', ['$http', '$q', function($http, $q){
	var getDirectories = function() {
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

		$http.get(url = '/getDirectories', config).then(function(response) {
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
	};
	
	var getSupportTypes = function() {
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

		$http.get(url = '/getSupportTypes', config).then(function(response) {
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
	};
	
	var updateSupportType = function(supportTypeDto) {
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

		$http.post(url = '/updateSupportType', data, config).then(function(response) {
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
	};
	
	var updateDirectories = function(pathList) {
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
		var data = JSON.stringify(pathList);

		$http.post(url = '/updateDirectories', data, config).then(function(response) {
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
	};
	
	var search = function(keyword){
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

		$http.get(url = '/search', config).then(function(response) {
			if (response.status == 200) {
				deferred.resolve(response.data);
			} else {
				deferred.reject(response.data);
			}
		}, function(response) {
			deferred.reject(response.data);
		}, function(response) {
			deferred.reject(response.data);
		});
		return deferred.promise;
		
	};

	return {getDirectories : getDirectories
			,updateDirectories : updateDirectories
			,search : search
			,getSupportTypes : getSupportTypes
			,updateSupportType : updateSupportType};
}]);
