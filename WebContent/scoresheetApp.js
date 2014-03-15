/**
 * 
 */
(function(appCode) {
	'use strict';
	var debug = true;
	if (debug === false) {
		debug = {
			log : function() {
			}
		};
	} else {
		debug = console;
	}
	appCode(jQuery, debug);
}(function($, debug) {
	'use strict';
	$(function() {
		'use strict';
		var $prev = $('#prev-button'), $next = $('#next-button'), 
			$nameSelection = $('#team-selection'),
				$namesInput = {
				home : $nameSelection.find('#home-team'),
				visitor : $nameSelection.find('#visitor-team')
			}, 
			$homeRoasterPage = $('#home-team-filling'), 
			$homeRoaster = $('#home-roaster'), 
			$visitorRoasterPage = $('#visitor-team-filling'), 
			$visitorRoaster = $('#visitor-roaster'),
			$eventFilling = $('#event-entry'),
			$eventFillingPage = $('#event-filling');

		$("[data-role='header'], [data-role='footer']").toolbar();
		// Step 1 select team names
		$nameSelection
			.on('pageshow',
				function() {
					debug.log('name selection: pageshow!');
					$prev.prop('disabled', true);
					$next.off('click').on('click', nameUpdate)
							.prop('disabled', true);
				})
			.on('pageinit',
				function() {
					debug.log('name selection: pageinit!');
					$nameSelection.on('validate', nameUpdate);
					$nameSelection.trigger('pageshow');
					$nameSelection
					.on(
							'change, keypress',
							'input',
							function() {
								if ($namesInput.home
										.val() !== ''
											&& $namesInput.visitor
											.val() !== '') {
									$next.prop(
											'disabled',
											false);
								} else {
									$next.prop(
											'disabled',
											true);
										}
								});
				});
		debug.log(window.location.hash);
		// if (window.location.hash !== ''){
		// $('body').pagecontainer('change', $nameSelection);
		// }
		if (window.location.hash === '') {
			$nameSelection.trigger('pageinit');
		}
		function nameUpdate() {
			debug.log('name update!');
			var homeName = $nameSelection.find('#home-team').val(), visitorName = $nameSelection
					.find('#visitor-team').val();
			$nameSelection.data('names', {
				home : homeName,
				visitor : visitorName
			});
			// now move on to the next screen
			$('body').pagecontainer('change', $homeRoasterPage, {
				transition : 'slide'
			});
		}
		function getHomeTeamName() {
			return $nameSelection.data('names').home || 'home';
		}
		function getVisitorTeamName() {
			return $nameSelection.data('names').visitor || 'visitor';
		}

		// Step 2 and 3 add home/visitor roaster
		function RoasterRow() {
			var $row = $('<div>', {
				'class' : 'ui-roaster-grid',
				'data-role' : 'player-input'
			});
			$('<div>', {
				'class' : 'ui-block-a'
			}).appendTo($row).append($('<input>', {
				type : 'number',
				min: '1', 
				max: '100',
				'data-role' : 'jersey-number',
				placeholder : '#??'
			}));
			$('<div>', {
				'class' : 'ui-block-b'
			}).appendTo($row).append($('<input>', {
				type : 'text',
				'data-role' : 'first-name',
				placeholder : 'firstname'
			}));
			$('<div>', {
				'class' : 'ui-block-c'
			}).appendTo($row).append($('<input>', {
				type : 'text',
				'data-role' : 'last-name',
				placeholder : 'lastname'
			}));
			$('<div>', {'class': 'ui-block-d'}).append($('<button>', {
				'class' : 'ui-custom-radius ui-btn ui-icon-minus ui-btn-icon-notext ui-corner-all'
			})).appendTo($row).click(function() {
				$row.remove();
			});
			$row.on('change.addRow, keypress.addRow', function() {
				if ($row.find('input[data-role="first-name"]').val()
						.trim() !== '') {
					$row.after(new RoasterRow());
					$row.off('change.addRow, keypress.addRow');
				}
			});
			return $row;
		}
		function updateRoaster($roaster, $currentPage, $nextPage) {
			// browse all the inputs
			var players = {}, $errorpopup = undefined;
			$roaster
				.find('div[data-role="player-input"]')
				.each(function(idx, elm) {
					var $elm = $(elm), 
						$num = $elm.find('input[data-role="jersey-number"]'), 
						$fstname = $elm.find('input[data-role="first-name"]'), 
						$lstname = $elm.find('input[data-role="last-name"]');
					if ($fstname.val().trim() !== '') {
						if (players[$num.val()] !== undefined) {
							// several players with the same
							// number
							$errorpopup = $('<div>', {
								'data-role' : 'popup'
							}).append(
								'<p> Several players have the same number in this team </p>');
							$('body').pagecontainer(
									'getActivePage')
									.append($errorpopup);
							$errorpopup.popup().popup('open')
								.on('popupafterclose',
									function() {
										$errorpopup.remove();
								});
							return;
						} else {
							// let's move on
							players[$num.val()] = {
								number : $num.val().trim(),
								firstName : $fstname.val().trim(),
								lastName : $lstname.val().trim()
							};
						}
					} // else ignore it
				});
			if ($errorpopup === undefined) {
				$currentPage.data('roaster', players);
				$('body').pagecontainer('change', $nextPage, {
					transition : 'slide'
				});
			}
		}

		$homeRoasterPage.on('pageshow', function() {
			$prev.prop('disabled', false).off('click')
				.on('click',
					function(e) {
						e.preventDefault();
						e.stopPropagation();
						$('body').pagecontainer('change',
								$nameSelection, {
									transition : 'slide',
									reverse : true
								});
					});
			$next.prop('disabled', false).off('click')
				.on('click',
					function(e) {
						e.preventDefault();
						e.stopPropagation();
						updateRoaster($homeRoaster, $homeRoasterPage, $visitorRoasterPage);
					});
		}).on('pageinit', function() {
			$homeRoaster.append(new RoasterRow());
		});
		$visitorRoasterPage.on('pageshow', function() {
			$prev.prop('disabled', false).off('click').on(
					'click',
					function(e) {
						e.preventDefault();
						e.stopPropagation();
						$('body').pagecontainer('change',
								$homeRoasterPage, {
									transition : 'slide',
									reverse : true
								});
					});
			$next.prop('disabled', false).off('click').on('click',
					function(e) {
						e.preventDefault();
						e.stopPropagation();
						updateRoaster($visitorRoaster, $visitorRoasterPage, $eventFillingPage);
					});
		}).on('pageinit', function() {
			$visitorRoaster.append(new RoasterRow());
		});
		function getHomeRoaster() {
			return $homeRoasterPage.data('roaster');
		}
		function getVisitorRoaster() {
			return $visitorRoasterPage.data('roaster');
		}
		
		//final step: adding the events and generate the report
		function EventRow() {
			var $row = $('<div>', {'class': 'ui-event-grid', 'data-role': 'event-input'});
			$('<div>', {'class': 'ui-block-a'}).append(
				$('<input>', {type: 'time', 'data-role': 'event-time'})
			).appendTo($row);
			$('<div>', {'class': 'ui-block-b'}).append(
				$('<select>', {'data-native-menu': 'false', 'data-role': 'event-type'}).append(
//					$('<option>', {value: ''}).text('Event'),
//					$('<option>', {value: 'penalty'}).text('Penalty'),
					$('<option>', {value: 'goal', 'selected': true}).text('Goal'))
			).appendTo($row);
			$('<div>', {'class': 'ui-block-c'}).append(
				"For team ", 
				$('<select>', {'data-native-menu': 'false', 'data-mini': 'true', 'data-role': 'event-team'}).append(
					$('<option>', {value: 'home', 'selected': true}).text(getHomeTeamName()),
					$('<option>', {value: 'visitor'}).text(getVisitorTeamName())).on('change', updateSelections)
					.trigger('change')
					,
				" scored by #", 
				$('<select>', {'data-mini': 'true', 'data-role': 'scorer'}), 
				' assisted by #',
				$('<select>', {'data-mini': 'true', 'data-role': 'first-assist'}), 
				' and #',
				$('<select>', {'data-mini': 'true', 'data-role': 'second-assist'})
			).appendTo($row);
			$row.on('change.lastevent', function() {
				$row.off('change.lastevent');
				$row.after(new EventRow());
				setTimeout(function() { 
					$row.find('select[data-role="event-team"]').trigger('change');
				}, 0);
			});
			return $row;
		}
		function updateSelections() {
			var $option, players, number = null, $playerSelecters = $(this).nextAll('select');
			if ( $(this).find('option:selected').val() === 'home') {
				players = getHomeRoaster();
			} else {
				players = getVisitorRoaster();
			}
			$playerSelecters.empty();
			$option = $('<option>', {value: ''}).text('no one');
			$playerSelecters.append($option);
			for (number in players) {
				if (players.hasOwnProperty(number)){
					$option = $('<option>', {value: number}).text(number + " " + players[number].firstName.charAt(0) + ". " + players[number].lastName);
					$playerSelecters.append($option);
				}
			}
		}
		$eventFillingPage.on('pageinit', function () {
			$eventFilling.append(new EventRow());
		}).on('pageshow', function () {
			$prev.prop('disabled', false).off('click')
				.on('click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					$next.text('Next');
					$('body').pagecontainer('change',
							$homeRoasterPage, {
								transition : 'slide',
								reverse : true
							});
				});
			$next.text('Generate').prop('disabled', false).off('click')
				.on('click', function(e) {
					e.preventDefault();
					e.stopPropagation();
					updateEvents();
					generateReport();
				});
		});
		function updateEvents() {
			var events = [];
			$eventFilling.find('div[data-role="event-input"]').each(function (idx, elm) {
				var evt = {}, $elm = $(elm);
				if($elm.find('input[data-role="event-time"]').val() !== '') {
					evt.time = $elm.find('input[data-role="event-time"]').val().trim();
					evt.type = $elm.find('select[data-role="event-type"]').find('option:selected').val().trim();
					if (evt.type !== 'goal') {
						debug.log('unrecognized event type: %s', evt.type);
					} else {
						evt.info = {};
						evt.info.team = $elm.find('select[data-role="event-team"]').find('option:selected').val();
						evt.info.scorer = $elm.find('select[data-role="scorer"]').find('option:selected').val();
						evt.info.firstAssist = $elm.find('select[data-role="first-assist"]').find('option:selected').val();
						evt.info.secondAssist = $elm.find('select[data-role="second-assist"]').find('option:selected').val();
						events.push(evt);
					}
				}
			});
			$eventFillingPage.data('events', events);
		}
		function generateReport() {
			var jsonMsg = {};
			jsonMsg.hometeam = {
				name: getHomeTeamName(),
				players: map2array(getHomeRoaster())	
			};
			jsonMsg.visitorteam = {
				name: getVisitorTeamName(),
				players: map2array(getVisitorRoaster())
			};
			jsonMsg.events = $eventFillingPage.data('events');
			debug.log('about to send the following message: ');
			debug.log(jsonMsg);
			$('<form>', {action: './api/generate.pdf', method: 'POST', 'data-ajax': false}).appendTo($eventFillingPage)
				.append($('<input>', {type: 'hidden', name: 'data', value: JSON.stringify(jsonMsg)}))
				.submit();
//			$.ajax({
//				url: './api/generate',
//				type: 'POST',
//				data: { data: JSON.stringify(jsonMsg)}
//			}).fail(function () {
//				debug.log('something went wrong!');
//			}).done(function (data) {
//				debug.log('yeeepeee');
//			});
		}
		function map2array(map) {
			var toReturn = [], idx = null;
			for (idx in map) {
				if (map.hasOwnProperty(idx)) {
					toReturn.push(map[idx]);
				}
			}
			return toReturn;
		}
	});
}));