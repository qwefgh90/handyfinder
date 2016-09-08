define(['angular', 'angularRoute', 'angularSanitize', 'angularAnimate', 'angularBootstrap'
        , 'ngContextmenu', 'indexModel', 'webSocketModel', 'searchModel', 'optionModel'], function(angular){
	var app = angular.module('handyfinderwebapp', ['ngRoute', 'ngSanitize', 'ngAnimate', 'ui.bootstrap', 'ngContextMenu', 'IndexModel', 'WebSocketModel', 'SearchModel', 'OptionModel']);
	app.run(['OptionModel', '$log', function(OptionModel, $log){
		OptionModel.getOptions();
	}]);
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
		})
		//라우트 설정객체에 controller 속성을 통하여 해당 화면에 연결되는 컨트롤러 이름을 설정할 수 있다.
		.otherwise({
			redirectTo : '/search'
		});
		//otherwise 메소드를 통하여 브라우저의 URL이 $routeProivder에서 정의되지 않은 URL일 경우에 해당하는 설정을 할 수 있다. 여기선 ‘/home’으로 이동시키고 있다.
	});
	app.controller('MainApplicationController', ['$location', '$scope','NativeService', '$log' , '$timeout',
	                                             function($location, $scope, NativeService, $log, $timeout) {
		$scope.path = '';
		$scope.go = function(path) {
			$location.path(path);
		};
		$scope.$on('$routeChangeStart', function(next, current) {
			$scope.path = $location.path().substring(1);
		});

		$scope.initGUIService = function(){
			if(NativeService.isConnected() == false){
				var promise = NativeService.connect();
				promise.then(function(frame) {
					$log.info(frame);
					$scope.openBrowser = function(){
						NativeService.openHome();
					};
				}, function(error) {
					$timeout(function() {$scope.initGUIService();}, 2000);//connect again
					$log.error(error);
				}, function(noti) {
					$log.info(noti);
				});
			}else{
				$scope.openBrowser = function(){
					NativeService.openHome();
				};
			}
		};
		
		$scope.initGUIService();
	}]);

	app.controller('searchController', ['$log', '$scope', '$timeout','$sce', 'NativeService', 'SearchModel','OptionModel',
	                                    function($log, $scope, $timeout, $sce, NativeService, SearchModel, OptionModel) {
		$scope.searchModel = SearchModel.model;
		$scope.optionModel = OptionModel.model;
		$scope.isCollapsed = true;
		
		$scope.search = function(keyword){
			SearchModel.search(keyword);
		};

		$scope.updateOption = function(){
			OptionModel.updateOptions();
		};

		$scope.initGUIService = function(){
			if(NativeService.isConnected() == false){
				var promise = NativeService.connect();
				promise.then(function(frame) {
					$log.info(frame);
					$scope.open = function(path){
						NativeService.openDirectory(path);
					};
					$scope.openFile = function(path){
						NativeService.openFile(path);
					};
				}, function(error) {
					$timeout(function() {$scope.initGUIService(); }, 2000); //connect again
					$log.error('[handy]'+error);
				}, function(noti) {
					$log.info('[handy]'+noti);
				});
			}else{
				$scope.open = function(path){
					NativeService.openDirectory(path);
				};
				$scope.openFile = function(path){
					NativeService.openFile(path);
				};
			}
		};
		
		$scope.initGUIService();
	}]);

	app.constant('RUNNING_INTERVAL', 10000);
	app.controller('indexController', ['$q','$log', '$timeout', '$location', '$scope', '$interval', 'Path', 'ProgressService', 'IndexModel', 'OptionModel', 'SupportTypeModel', 'SupportTypeUI', 'RUNNING_INTERVAL',
	                                   function($q, $log, $timeout, $location, $scope, $interval, Path, progressService, IndexModel, OptionModel, SupportTypeModel, SupportTypeUI, RUNNING_INTERVAL) {
		$scope.indexModel = IndexModel.model;
		$scope.optionModel = OptionModel.model;
		$scope.supportTypeModel = SupportTypeModel.model;
		$scope.supportTypeUI = new SupportTypeUI(SupportTypeModel);

		var directoriesPromise = IndexModel.getDirectories();	

		$scope.indexModel.select_toggle = false;
		$scope.changeSearchKeyword = function(searchedTypeKeyword){
			$scope.supportTypeUI.changeSearchKeyword(searchedTypeKeyword);
		};

		$scope.nextSearch = function(searchedTypeKeyword){
			$scope.supportTypeUI.nextSearch(searchedTypeKeyword);
		}

		$scope.loadMore = function(){
			$scope.supportTypeUI.loadMore();
		};

		$scope.save = function() {
			var deferred = $q.defer();
			var promise = IndexModel.updateDirectories();
			promise.then(function() {
				//$scope.indexModel.index_progress_status.addAlertQ(0);
				deferred.resolve();
				$log.debug('successful update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
				$log.error('fail to update a list of path');
			}, function() {
				$scope.indexModel.index_progress_status.addAlertQ(1);
				deferred.reject();
			});
			return deferred.promise;
		};

		$scope.saveOption = function() {
			OptionModel.updateOptions();
		}

		$scope.startWatch = function() {
			$scope.$watchCollection('indexModel.pathList', function(newNames, oldNames) {
				$scope.save();
			});
		};

		$scope.selectDirectory = function(originalPath) {
			progressService.openDirectoryDialog();
		};

		$scope.addDirectory = function(path) {
			var returnedPath = path;
			if (returnedPath != '') {
				var path = Path.createInstance(returnedPath);
				$scope.indexModel.pathList.push(path);
				$scope.save();
				$log.debug('pushed path: '+ returnedPath);
			}
		};

		$scope.refreshCount = function(){
			var countPromise = IndexModel.getDocumentCount();
			countPromise.then(function(count){
				$scope.indexModel.indexDocumentCount = count;
				$log.log('indexed Count : ' + count);
			}, function(){}, function(){});
		};

		{ //context menu handler bind
			$scope.enableToggle = function(path) {
				path.used = !path.used;
			};

			$scope.recursivelyToggle = function(path) {
				path.recursively = !path.recursively;
			};

			$scope.remove = function(path) {
				$timeout(function() {
					var index = $scope.indexModel.pathList.indexOf(path);
					if (index > -1) {
						$scope.indexModel.pathList.splice(index, 1);
					}
				}, 100);
			};
		}

		$scope.run = function() {
			var promise = $scope.save();
			promise.then(function(){
				$scope.indexModel.running = 'RUNNING'
					progressService.sendStartIndex();
				$scope.indexModel.intervalStopObject = $interval(function(){
					if($scope.indexModel.intervalTurn % 2 == 0){
						$log.debug('update index...');
						if($scope.indexModel.state != 'TERMINATE'){
							$scope.indexModel.intervalTurn = $scope.indexModel.intervalTurn - 1; //next time call sendUpdateIndex() again;
							$log.info('stopping update index... other job still working');
						}
						else
							progressService.sendUpdateIndex();
					}else{
						$log.debug('start index...');
						if($scope.indexModel.updateSummary.state != 'TERMINATE'){
							$scope.indexModel.intervalTurn = $scope.indexModel.intervalTurn - 1; //next time call sendStartIndex() again;
							$log.info('stopping start index... other job still working');
						}
						else
							progressService.sendStartIndex();
					}

					$scope.indexModel.intervalTurn = $scope.indexModel.intervalTurn + 1;
					if($scope.indexModel.intervalTurn == 100)
						$scope.indexModel.intervalTurn = 0;
				}, RUNNING_INTERVAL);
			},function(){},function(){});
		};

		$scope.stop = function(){
			if($scope.indexModel.intervalStopObject == undefined){
				$log.info('not started yet');
				return;
			}
			if($scope.indexModel.running != 'RUNNING'){
				$log.error('illegal state. not running');
				return;
			}
			$log.info('stopping index...');
			$interval.cancel($scope.indexModel.intervalStopObject);
			$scope.indexModel.intervalStopObject = undefined;
			$scope.indexModel.running = 'READY';		// change state
			progressService.sendStopIndex();
		}

		$scope.updateType = function(obj) {
			var promise = SupportTypeModel.updateSupportType(obj);
			promise.then(function(){
				$log.debug('successful update ' + obj.type + ':' + obj.used);
			},function(){
				$log.error('fail to update ');},function(){});
		};

		//click top element of check boxes
		$scope.toggleTopcheckbox = function(){
			$log.debug('toggle top checkbox : ' + $scope.indexModel.select_toggle);
			for (var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				SupportTypeModel.model.supportTypes[i].used = $scope.indexModel.select_toggle;
			}
			$scope.updateSupportTypeList(); //update to api server
		}
		
		$scope.updateSupportTypeList = function() {
			SupportTypeModel.updateSupportTypeList();
		}

		$scope.initProgressService = function(){
			if(progressService.isConnected() == false){
				var promise = progressService.connect();
				promise.then(function(frame) {
					$log.debug(frame);
					var progressPromise = progressService.subProgress();
					progressPromise.then(function() {
					}, function(msg) {
						$log.error(msg);
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
						$log.debug(progressObject.processIndex + ", " + progressObject.totalProcessCount + ", " + progressObject.processPath + ", " + progressObject.state);
						$scope.refreshCount();
					});
					var updatePromise = progressService.subUpdate();
					updatePromise.then(function() {
					}, function(msg) {
						$log.error(msg);
					}, function(summaryObject) {
						if(summaryObject.state == 'START')
							return;
						$scope.indexModel.updateSummary.countOfDeleted = summaryObject.countOfDeleted;
						$scope.indexModel.updateSummary.countOfExcluded = summaryObject.countOfExcluded;
						$scope.indexModel.updateSummary.countOfModified = summaryObject.countOfModified;
						$scope.indexModel.updateSummary.state = summaryObject.state;

						$scope.indexModel.index_progress_status.setMsgAlertQ(5, 'Update summary -> deleted files : ' + summaryObject.countOfDeleted 
								+ ', non contained files : ' + summaryObject.countOfExcluded + ', modified files : ' + summaryObject.countOfModified);
						$scope.indexModel.index_progress_status.addAlertQ(5);
						$log.debug(summaryObject.countOfDeleted + ", " + summaryObject.countOfExcluded + ", " + summaryObject.countOfModified);
						$scope.refreshCount();
					});

					var guiDirPromise = progressService.subGuiDirectory();
					guiDirPromise.then(function(){},
							function(msg){$log.error(msg);},
							function(path){
								$log.info('selected path : ' + path);
								$scope.addDirectory(path);
							});

				}, function(error) {
					$timeout(function() { $scope.initProgressService(); }, 2000); //connect again
					$log.error(error);
				}, function(noti) {
					$log.debug(noti);
				});
			}
		}
		$scope.initProgressService();

		//pathlist from apiserver
		directoriesPromise.then(function(msg) {
			$log.debug('directories loaded');
			$scope.indexModel.pathList = msg;
			$scope.indexModel.index_progress_status.addAlertQ(2);
			$scope.startWatch();
		}, function(msg) {
			$log.error('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		}, function(msg) {
			$log.debug('directories fail to load');
			$scope.indexModel.index_progress_status.addAlertQ(3);
			$scope.startWatch();
		});

		var typePromise = SupportTypeModel.getSupportTypes();
		typePromise.then(function(msg) {
			$log.debug('supportTypes loaded : ' + msg.length);
		}, function(msg) {
			$log.error('supportTypes fail to load');
		}, function(msg) {
			$log.debug('supportTypes notify');
		});

		$scope.$watch('supportTypeModel.supportTypes', function(){
			for(var i=0; i<SupportTypeModel.model.supportTypes.length; i++){
				if(SupportTypeModel.model.supportTypes[i].used == false){
					$scope.indexModel.select_toggle = false;
					return;
				}
			}
			$scope.indexModel.select_toggle = true;
		}, true);

		$scope.refreshCount();
	}]);

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
	
	//https://stackoverflow.com/questions/12790854/angular-directive-to-scroll-to-a-given-item/28369575#28369575 written by Mat
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
	return app;
});