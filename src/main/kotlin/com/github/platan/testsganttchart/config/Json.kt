package com.github.platan.testsganttchart.config

import org.gradle.api.model.ObjectFactory
import javax.inject.Inject

abstract class Json @Inject constructor(objectFactory: ObjectFactory) : Format(objectFactory, "json")