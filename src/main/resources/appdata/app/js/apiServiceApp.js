var app = angular.module('apiServiceApp',[]);


app.factory('apiService', ['$http', '$q', '$log', function($http, $q, $log){
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
	};

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

		$http.get(url = '/supportTypes', config).then(function(response) {
			if (response.status == 200) {
				deferred.resolve(response.data);
			} else {
				deferred.reject('supportTypes() fail');
			}
		}, function(response) {
			deferred.reject(response.data);
		}, function(response) {
			deferred.reject(response.data);
		});
		return deferred.promise;
		// then(successCallback, errorCallback, notifyCallback)
	};
	
	var updateSupportTypeList = function(list) {
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

		$http.get(url = '/documents', config).then(function(response) {
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

	var getDocumentContent = function(pathString, keyword){
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

	var getOptions = function() {
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

		$http.get(url = '/options', config).then(function(response) {
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
	
	var updateOptions = function(option) {
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
		var data = JSON.stringify(option);

		$http.post(url = '/options', data, config).then(function(response) {
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
	
	return {getDirectories : getDirectories
			,updateDirectories : updateDirectories
			,search : search
			,getSupportTypes : getSupportTypes
			,updateSupportType : updateSupportType
			,updateSupportTypeList : updateSupportTypeList
			,updateOptions : updateOptions
			,getOptions : getOptions
			,getDocumentCount : getDocumentCount
			,getDocumentContent : getDocumentContent};
}]);
