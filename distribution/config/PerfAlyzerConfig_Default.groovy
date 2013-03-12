// the number of threads used for processing
threadCount = 8

// the locale for reporting, affects language and number formatting
locale = 'en'
warmUpSeconds = 0
reportsBaseUrl = null as String
maxHistoryItems = 20

email {
	enabled = false
	from = null
	to = []
	smtp {
		host = 'smtp-relay.mgm-tp.com'
		// port = 
		// ssl = true
		// auth = true
		// username = ''
		// password = ''
	}
	/*
	subjects {
		
	}
	*/
	
	// maxHistoryItems = 20
}

/* Formatting happens during report preparation, i. e. patterns are matched against binned files.
 * Only the file names (not the complete path) are matched against the patterns. */
formats {
	perfmonMemSwap {
		pattern = ~/\[perfmon\]\[(?:mem|swap)\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.medianKiB']
	}
	perfmonMemIO {
		pattern = ~/\[perfmon\]\[io_(?:r|w)\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.medianB']
	}
	perfmonCPU {
		pattern = ~/\[perfmon\]\[(?:cpu_X|java)\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.meanPercent']
	}
	measuringDistribution {
		pattern = ~/\[measuring\]\[[^]]+\]\[distribution\].*/
		unitX = 'axis.label.timeMillis'
		unitY = ['axis.label.numCalls']
	}
	measuringExecutions {
		pattern = ~/\[measuring\]\[executions\].*/
		unitX = 'axis.label.timeMillis'
		unitY = ['axis.label.medianExecutionTimeMillis']
	}
	measuringExecutionsPerMinute {
		pattern = ~/\[measuring\]\[execMin\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.executionsPerMinute']
	}
	measuringExecutionsPer10Minutes {
		pattern = ~/\[measuring\]\[exec10Min\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.executionsPer10Minutes']
	}
	measuringRequestsPerMinute {
		pattern = ~/\[measuring\]\[requests\]\[60000].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.requestsPerMinute']
	}
	measuringRequestsPerSecond {
		pattern = ~/\[measuring\]\[requests\]\[1000].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.requestsPerSecond']
	}
	measuringErrors {
		pattern = ~/\[measuring\]\[errors\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.numErrors']
	}
	gcLogs {
		pattern = ~/\[gclog\].*/
		unitX = 'axis.label.timeSeconds'
		unitY = ['axis.label.MiB', 'axis.label.timeMillis']
	}
}

reportContents {
	/* By default, files for the report are considered in natural order, i. e. they have priority 0.
	 * Sorting may be changed by giving certain files a higher priority. Files will be sorted
	 * by priority in the report with descending order. If a file matches a pattern in this list it
	 * gets a priority that is equal to the element index of the pattern in the list. The list is
	 * processed in reverse order. Processing is stopped as soon as a pattern matches. */
	priorities = [
		~/.*(?<!comparison)[\\\\/].*/,
		~/global[\\\\/].*/,
		~/global[\\\\/]\[perfmon\]\[cpu_X\].*/,
		~/global[\\\\/]\[measuring\].*/,
		~/global[\\\\/]\[measuring\]\[exec10Min\].*/,
		~/global[\\\\/]\[measuring\]\[execMin\].*/,
		~/global[\\\\/]\[measuring\]\[requests\].*/,
		~/global[\\\\/]\[measuring\]\[executions\].*/
	]

	// Files matching any of these expressions will be excluded from the report.
	exclusions = [
		~/global[\\\\/]\[perfmon\]\[java\].*/,
		~/.*(?<!global)[\\\\/]\[perfmon\]\[(?:mem|swap|cpu_X|io_(?:r|w))\].*/
	]	
}
