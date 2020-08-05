/**
 * Copyright © 2014 - 2020 Lightbend, Inc. All rights reserved. [http://www.lightbend.com]
 */

package com.lightbend.training.coffeehouse

import org.scalactic.TypeCheckedTripleEquals
import org.scalatest.Inspectors
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec

abstract class BaseSpec extends AnyWordSpec with Matchers with TypeCheckedTripleEquals with Inspectors
