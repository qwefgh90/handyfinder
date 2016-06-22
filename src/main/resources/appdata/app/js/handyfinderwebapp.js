var app = angular.module('handyfinderwebapp', ['ngRoute', 'ngSanitize', 'ngAnimate', 'ui.bootstrap', 'apiServiceApp', 'ngContextMenu', 'indexModelApp', 'websocketModelApp']);

app.config(function($routeProvider) {
	//Module의 config API를 사용하면 서비스 제공자provider에 접근할 수 있다. 여기선 $route 서비스 제공자를 인자로 받아온다.
	$routeProvider
	//$routeProvider의 when 메소드를 이용하면 특정 URL에 해당하는 라우트를 설정한다. 이때 라우트 설정객체를 전달하는데 <ng-view>태그에 삽입할 탬플릿에 해당하는 url을 설정객체의 templateUrl 속성으로 정의한다.
	.when('/search', {
		templateUrl : 'views/search.html',
		controller : 'searchController'
	}).when('/index', {
		templateUrl : 'views/index.html',
		controller : 'indexController'
	}).when('/setting', {
		templateUrl : 'views/setting.html',
		controller : 'settingController'
	})
	//라우트 설정객체에 controller 속성을 통하여 해당 화면에 연결되는 컨트롤러 이름을 설정할 수 있다.
	.otherwise({
		redirectTo : '/search'
	});
	//otherwise 메소드를 통하여 브라우저의 URL이 $routeProivder에서 정의되지 않은 URL일 경우에 해당하는 설정을 할 수 있다. 여기선 ‘/home’으로 이동시키고 있다.
});
app.controller('mainApplicationController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.path = '';
	$scope.go = function(path) {
		$location.path(path);
	};
	$scope.$on('$routeChangeStart', function(next, current) {
		$scope.path = $location.path().substring(1);
	});
}]);

app.controller('searchController', ['$location','$log', '$scope', '$timeout', 'apiService', 'Document','$sce', 'GUIService', 'SearchModel',
function($location, $log, $scope, $timeout, apiService, Document, $sce, GUIService, SearchModel) {
	$scope.searchModel = SearchModel.model;
	$scope.searchFlag = false;
	$scope.search = function(keyword){
		var milliseconds = (new Date).getTime();
		if($scope.searchFlag == true)
			return;
		var promise = apiService.search(keyword);
		$scope.searchFlag = true;
		$scope.searchModel.searchTryCount = $scope.searchModel.searchTryCount + 1;
		promise.then(function(json){
			var toMiliseconds = (new Date).getTime();
			$scope.searchModel.searchResult = [];
			for(var i = 0 ; i < json.length ; i ++){
				var data = json[i];
				var document = new Document(data.createdTime, data.modifiedTime, data.title, data.pathString, data.contents, data.parentPathString, data.fileSize, data.mimeType);
				$scope.searchModel.searchResult.push(document);
			}
			$scope.searchModel.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000
			$scope.searchFlag = false;
		},function(){
			var toMiliseconds = (new Date).getTime();
			$scope.searchFlag = false;
			$log.log('search failed');
			$scope.searchModel.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000}
		,function(){
			var toMiliseconds = (new Date).getTime();
			$scope.searchFlag = false;
			$scope.searchModel.searchTime = (toMiliseconds * 1.0 - milliseconds * 1.0) / 1000});
	};

	$scope.initGUIService = function(){
		if(GUIService.isConnected() == false){
			var promiseArray = GUIService.connect();
			var promise = promiseArray[0];
			promise.then(function(frame) {
				$log.log('[handy]'+frame);
				GUIService.subDirectory();
				$scope.open = function(path){
					GUIService.openDirectory(path);
				};
				$scope.open_file = function(path){
					GUIService.openFile(path);
				};
			}, function(error) {
				$log.log('[handy]'+error);
			}, function(noti) {
				$log.log('[handy]'+noti);
			});
			var failPromise = promiseArray[1];
			failPromise.then(function(){},function(){$timeout(function() {
				$scope.initGUIService(); //connect again
			}, 2000);
			},function(){});
		}else{
			$scope.open = function(path){
				GUIService.openDirectory(path);
			};
			$scope.open_file = function(path){
				GUIService.openFile(path);
			};
		}
	};
	$scope.initGUIService();
}]);

app.constant('LOAD_MORE_COUNT', 500);
app.constant('RUNNING_INTERVAL', 60000);
app.controller('indexController', ['$q','$log', '$timeout', '$location', '$scope', '$interval', 'apiService', 'Path', 'ProgressService', 'IndexModel', 'OptionModel', 'LOAD_MORE_COUNT', 'RUNNING_INTERVAL',
function($q, $log, $timeout, $location, $scope, $interval, apiService, Path, progressService, IndexModel, OptionModel, LOAD_MORE_COUNT, RUNNING_INTERVAL) {
	$scope.indexModel = IndexModel.model;
	$scope.optionModel = OptionModel.model;
	
	var directoriesPromise = apiService.getDirectories();	
	var optionPromise = apiService.getOptions();
	
	$scope.indexModel.select_toggle = false;
	$scope.totalDisplayed = 0;
	$scope.searchedTypeKeyword = '';
	$scope.searchedType = '';
	$scope.enterHitCount = 0;
	$scope.changeSearchKeyword = function(searchedTypeKeyword){
		$scope.enterHitCount = 0;
		if(searchedTypeKeyword == '')
			return;
		for(var i = 0; i < $scope.totalDisplayed; i++){
//			$log.log($scope.indexModel.supportTypes[i].type);
			if($scope.indexModel.supportTypes[i].type.indexOf(searchedTypeKeyword) > -1){
				$scope.searchedType = $scope.indexModel.supportTypes[i].type;
				$log.log($scope.indexModel.supportTypes[i].type + ' is matched');
				$log.log('0 hit');
				return;
				
			}
		}
//		$scope.searchedType = $scope.indexModel.supportTypes[i].type;
	};
	
	$scope.nextSearch = function(searchedTypeKeyword){
		$scope.enterHitCount += 1;
		var enterHitCount = $scope.enterHitCount;
		var searchStack = [];
		var offsetCounter = 0;
		var searchedType = '';
		if(searchedTypeKeyword == '')
			return;
		for(var i = 0; i < $scope.totalDisplayed; i++){
			//matched
			if($scope.indexModel.supportTypes[i].type.indexOf(searchedTypeKeyword) > -1){
				searchedType = $scope.indexModel.supportTypes[i].type;
				searchStack.push(searchedType);
			}
		}
		//enter overflow
		if(searchStack.length <= $scope.enterHitCount){
			$scope.searchedType = searchStack[0]
			$scope.enterHitCount = 0;
			$log.log('enter hit overflow');
			$log.log($scope.enterHitCount + ' offset hit');
		}else{
			$scope.searchedType = searchStack[$scope.enterHitCount];
			$log.log($scope.enterHitCount + ' offset hit');
		}
		
	}
	
	$scope.loadMore = function(){
		$scope.totalDisplayed = $scope.totalDisplayed + LOAD_MORE_COUNT;
		if($scope.indexModel.supportTypes.length < $scope.totalDisplayed){
			$scope.totalDisplayed = $scope.indexModel.supportTypes.length;
		}
	};
	
	$scope.save = function() {
		var deferred = $q.defer();
		var promise = apiService.updateDirectories($scope.indexModel.pathList);
		promise.then(function() {
			//$scope.indexModel.index_progress_status.addAlertQ(0);
			deferred.resolve();
		}, function() {
			$scope.indexModel.index_progress_status.addAlertQ(1);
			deferred.reject();
		}, function() {
			$scope.indexModel.index_progress_status.addAlertQ(1);
			deferred.reject();
		});
		return deferred.promise;
	};

	$scope.saveOption = function() {
		var promise = apiService.updateOptions($scope.optionModel.option);
		promise.then(function() {
			$log.log('finish to save option.');
		}, function() {
			$log.log('fail to save option.');
		}, function() {
			$log.log('fail to save option.');
		});
	}
	
	$scope.startWatch = function() {
		$scope.$watchCollection('pathList', function(newNames, oldNames) {
			$scope.save();
			$log.log('update a list of indexes in server');
		});
	};

	$scope.selectDirectory = function() {
		if(guiService == undefined){
			$log.log('you can\'t javafx method in browser');
			return '';
		}
		var path = guiService.openDialogAndSelectDirectory();
		$log.log(path);
		return path;
	};

	$scope.addDirectory = function() {
		var returnedPath = $scope.selectDirectory();
		if (returnedPath != '') {
			var path = Path.createInstance(returnedPath);
			$scope.indexModel.pathList.push(path);
			$scope.save();
			alert('pushed path');
		}
	};

	$scope.enableToggle = function(path) {
		path.used = !path.used;
	};

	$scope.recursivelyToggle = function(path) {
		path.recursively = !path.recursively;
	};

	$scope.edit = function() {
		return $scope.selectDirectory();
	};

	$scope.remove = function(path) {
		$timeout(function() {
			var index = $scope.indexModel.pathList.indexOf(path);
			if (index > -1) {
				$scope.indexModel.pathList.splice(index, 1);
			}
		}, 100);
	};

	$scope.run = function() {
		var promise = $scope.save();
		promise.then(function(){
			progressService.sendStartIndex();
			$scope.indexModel.intervalStopObject = $interval(function(){
				if($scope.indexModel.intervalTurn % 2 == 0){
					$log.log('update index...');
					progressService.sendUpdateIndex();
				}else{
					$log.log('start index...');
					progressService.sendStartIndex();
				}
				
				$scope.indexModel.intervalTurn = $scope.indexModel.intervalTurn + 1;
				if($scope.indexModel.intervalTurn == 100)
					$scope.indexModel.intervalTurn = 0;
			}, RUNNING_INTERVAL);
		},function(){},function(){});
	};
	
	$scope.stop = function(){
		if($scope.indexModel.intervalStopObject != undefined){
			$log.log('stopping index...');
			progressService.sendStopIndex();
			$interval.cancel($scope.indexModel.intervalStopObject);
			$scope.indexModel.intervalStopObject = undefined;
			$scope.indexModel.running = 'WAITING';
		}
	}
	
	$scope.updateType = function(obj) {
		var promise = apiService.updateSupportType(obj);
		promise.then(function(){
				$log.log('successful update '+obj.type + ':' + obj.used);
			},function(){
				$log.log('fail to update ');},function(){});
	};
	
	//click top element of check boxes
	$scope.toggleTopcheckbox = function(){
		$log.log('toggle top checkbox : ' + $scope.indexModel.select_toggle);
		for (var i=0; i<$scope.indexModel.supportTypes.length; i++){
			$scope.indexModel.supportTypes[i].used = $scope.indexModel.select_toggle;
		}
		$scope.updateSupportTypeList(); //update to api server
	}
	
	$scope.updateSupportTypeList = function() {
		var promise = apiService.updateSupportTypeList($scope.indexModel.supportTypes);
		promise.then(function(){
			$log.log('successful update support list');
		},function(){
			$log.log('fail to update ');},function(){});
	}
	
	$scope.initProgressService = function(){
		if(progressService.isConnected() == false){
			var promiseArray = progressService.connect();;
			var promise = promiseArray[0];
			promise.then(function(frame) {
				$log.log('[handy]'+frame);
				var progressPromise = progressService.subProgress();
				progressPromise.then(function() {
					}, function(msg) {
						$log.log(msg);
					}, function(progressObject) {
						if (progressObject.state == 'START')
							$scope.indexModel.index_progress_status.progressBarVisible = true;
						else if (progressObject.state == 'TERMINATE'){
							$scope.indexModel.index_progress_status.progressBarVisible = false;
							$scope.indexModel.index_progress_status.addAlertQ(4);
						}
						$scope.indexModel.processIndex = progressObject.processIndex;
						$scope.indexModel.processPath = progressObject.processPath;
						$scope.indexModel.totalProcessCount = progressObject.totalProcessCount;
						$scope.indexModel.state = progressObject.state;
						$scope.indexModel.index_progress_status.refreshState();
						$log.log(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
					});
				var updatePromise = progressService.subUpdate();
				updatePromise.then(function() {
				}, function(msg) {
					$log.log(msg);
				}, function(summaryObject) {
					$scope.indexModel.updateSummary.countOfDeleted = summaryObject.countOfDeleted;
					$scope.indexModel.updateSummary.countOfExcluded = summaryObject.countOfExcluded;
					$scope.indexModel.updateSummary.countOfModified = summaryObject.countOfModified;
					$scope.indexModel.updateSummary.state = summaryObject.state;
					$log.log(summaryObject.countOfDeleted + ", " + summaryObject.countOfExcluded + ", " + summaryObject.countOfModified);
				});
				
			}, function(error) {
				$log.log('[handy]'+error);
			}, function(noti) {
				$log.log('[handy]'+noti);
			});
			var failPromise = promiseArray[1];
			failPromise.then(function(){},function(){$timeout(function() {
				$scope.initProgressService(); //connect again
			}, 2000);
			},function(){});
		}
	}
	$scope.initProgressService();
	
	//pathlist from apiserver
	directoriesPromise.then(function(msg) {
		$log.log('directories loaded');
		$scope.indexModel.pathList = msg;
		$scope.indexModel.index_progress_status.addAlertQ(2);
		$scope.startWatch();
	}, function(msg) {
		$log.log('directories fail to load');
		$scope.indexModel.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	}, function(msg) {
		$log.log('directories fail to load');
		$scope.indexModel.index_progress_status.addAlertQ(3);
		$scope.startWatch();
	});
	
	//option from apiserver
	optionPromise.then(function(msg) {
		$scope.optionModel.option.limitCountOfResult = msg.limitCountOfResult;
		$scope.optionModel.option.maximumDocumentMBSize = msg.maximumDocumentMBSize;
		$log.log('option loaded ' + $scope.optionModel.option.limitCountOfResult + ', ' + $scope.optionModel.option.maximumDocumentMBSize);
	}, function(msg) {
		$log.log('option fail to load');
	}, function(msg) {
	});

	var typePromise;
	
	//type from apiserver
	if($scope.indexModel.supportTypes.length == 0){
		typePromise = apiService.getSupportTypes();
		typePromise.then(function(msg) {
			$scope.indexModel.supportTypes = msg;
			$scope.totalDisplayed = 100;	//minimum > 1000 
			$log.log('supportTypes loaded : ' + msg.length);
		}, function(msg) {
			$log.log('supportTypes fail to load');
		}, function(msg) {
		});
	}else{
		$scope.totalDisplayed = 100; //minimum > 1000 
	}
	
	$scope.$watch('indexModel.supportTypes', function(){
		for(var i=0; i<$scope.indexModel.supportTypes.length; i++){
			if($scope.indexModel.supportTypes[i].used == false){
				$scope.indexModel.select_toggle = false;
				return;
			}
		}
		$scope.indexModel.select_toggle = true;
	}, true);
	
	$scope.$watch('indexModel.running + indexModel.state + indexModel.updateSummary.state', function () {
		if($scope.indexModel.running == 'WAITING' && $scope.indexModel.state == 'TERMINATE' && $scope.indexModel.updateSummary.state == 'TERMINATE'){
			$log.log('INDEX WRTIE TERMINATE');
			$scope.indexModel.running = 'READY';
		}else if(($scope.indexModel.state != 'TERMINATE' || $scope.indexModel.updateSummary.state != 'TERMINATE')){
			$log.log('INDEX WRTIE START');
			$scope.indexModel.running = 'RUNNING';
		}
	});
	
}]);

app.controller('settingController', ['$location', '$scope', 'apiService',
function($location, $scope, apiService) {
	$scope.extension_status = {
		open : true
	};

}]);
/*
app.directive('focusMe', function($timeout, $parse) {
	//http://stackoverflow.com/questions/14833326/how-to-set-focus-on-input-field
	return {
		//scope: true,   // optionally create a child scope
		link : function(scope, element, attrs) {
			var model = $parse(attrs.focusMe);
			scope.$watch(model, function(value) {
				console.log('value=', value);
				if (value === true) {
					$timeout(function() {
						element[0].focus();
					});
				}
			});
			// to address @blesh's comment, set attribute value to 'false'
			// on blur event:
			element.bind('blur', function() {
				console.log('blur');
				scope.$apply(model.assign(scope, false));
			});
		}
	};
});*/
app.directive("compileHtml", function($parse, $sce, $compile) {
    return {
        restrict: "A",
        link: function (scope, element, attributes) {
 
            var expression = $sce.parseAsHtml(attributes.compileHtml);
 
            var getResult = function () {
                return expression(scope);
            };
 
            scope.$watch(getResult, function (newValue) {
                var linker = $compile(newValue);
                element.append(linker(scope));
            });
        }
    }
});
/*
https://stackoverflow.com/questions/12790854/angular-directive-to-scroll-to-a-given-item/28369575#28369575
written by Mat*/
app.directive('scrollIf', function () {
    var getScrollingParent = function(element) {
        element = element.parentElement;
        while (element) {
            if (element.scrollHeight !== element.clientHeight) {
                return element;
            }
            element = element.parentElement;
        }
        return null;
    };
    return function (scope, element, attrs) {
        scope.$watch(attrs.scrollIf, function(value) {
            if (value) {
                var sp = getScrollingParent(element[0]);
                var topMargin = parseInt(attrs.scrollMarginTop) || 0;
                var bottomMargin = parseInt(attrs.scrollMarginBottom) || 0;
                var elemOffset = element[0].offsetTop;
                var elemHeight = element[0].clientHeight;

                if (elemOffset - topMargin < sp.scrollTop) {
                    sp.scrollTop = elemOffset - topMargin;
                } else if (elemOffset + elemHeight + bottomMargin > sp.scrollTop + sp.clientHeight) {
                    sp.scrollTop = elemOffset + elemHeight + bottomMargin - sp.clientHeight;
                }
            }
        });
    }
});