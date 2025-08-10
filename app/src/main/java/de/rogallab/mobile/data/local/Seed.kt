package de.rogallab.mobile.data.local

import android.content.Context
import android.content.res.Resources
import android.graphics.BitmapFactory
import de.rogallab.mobile.R
import de.rogallab.mobile.data.local.io.deleteFileOnStorage
import de.rogallab.mobile.data.local.io.writeImageToStorage
import de.rogallab.mobile.domain.entities.Person
import de.rogallab.mobile.domain.utilities.logDebug
import de.rogallab.mobile.domain.utilities.newUuid
import kotlin.random.Random

class Seed(
   private val _context: Context,
) {
   var people: MutableList<Person> = mutableListOf<Person>()

   private val _resources: Resources = _context.resources
   private val _imagesUri = mutableListOf<String>()

   init {
      val firstNames = mutableListOf(
         "Arne", "Berta", "Cord", "Dagmar", "Ernst", "Frieda", "Günter", "Hanna",
         "Ingo", "Johanna", "Klaus", "Luise", "Martin", "Nadja", "Otto", "Patrizia",
         "Quirin", "Rebecca", "Stefan", "Tanja", "Uwe", "Veronika", "Walter", "Xaver",
         "Yvonne", "Zwantje")
      val lastNames = mutableListOf(
         "Arndt", "Bauer", "Conrad", "Diehl", "Engel", "Fischer", "Graf", "Hoffmann",
         "Imhoff", "Jung", "Klein", "Lang", "Meier", "Neumann", "Olbrich", "Peters",
         "Quart", "Richter", "Schmidt", "Thormann", "Ulrich", "Vogel", "Wagner", "Xander",
         "Yakov", "Zander")
      val emailProvider = mutableListOf("gmail.com", "icloud.com", "outlook.com", "yahoo.com",
         "t-online.de", "gmx.de", "freenet.de", "mailbox.org", "yahoo.com", "web.de")
      val random = Random(0)
      for (index in firstNames.indices) {
//         var indexFirst = random.nextInt(firstNames.size)
//         var indexLast = random.nextInt(lastNames.size)
         val firstName = firstNames[index]
         val lastName = lastNames[index]
         val email =
            "${firstName.lowercase()}." +
               "${lastName.lowercase()}@" +
               "$emailProvider.random()"
         val phone =
            "0${random.nextInt(1234, 9999)} " +
               "${random.nextInt(100, 999)}-" +
               "${random.nextInt(10, 9999)}"
         val person = Person(firstName, lastName, email, phone, null, newUuid())
         people.add(person)
      }

      // convert the drawables into image files
      val drawables = mutableListOf<Int>()
      drawables.add(0, R.drawable.man_1)
      drawables.add(1, R.drawable.man_2)
      drawables.add(2, R.drawable.man_3)
      drawables.add(3, R.drawable.man_4)
      drawables.add(4, R.drawable.man_5)
      drawables.add(5, R.drawable.man_6)
      drawables.add(6, R.drawable.woman_1)
      drawables.add(7, R.drawable.woman_2)
      drawables.add(8, R.drawable.woman_3)
      drawables.add(9, R.drawable.woman_4)
      drawables.add(10, R.drawable.woman_5)

      drawables.forEach { it: Int ->  // drawable id
         val bitmap = BitmapFactory.decodeResource(_resources, it)
         bitmap?.let { itbitm ->
            writeImageToStorage(_context, itbitm)?.let { uriPath: String? ->
               uriPath?.let { _imagesUri.add(uriPath) }
            }
         }
      }
      if (_imagesUri.size == 11) {
         people[0] = people[0].copy(imagePath = _imagesUri[0])
         people[1] = people[1].copy(imagePath = _imagesUri[6])
         people[2] = people[2].copy(imagePath = _imagesUri[1])
         people[3] = people[3].copy(imagePath = _imagesUri[7])
         people[4] = people[4].copy(imagePath = _imagesUri[2])
         people[5] = people[5].copy(imagePath = _imagesUri[8])
         people[6] = people[6].copy(imagePath = _imagesUri[3])
         people[7] = people[7].copy(imagePath = _imagesUri[9])
         people[8] = people[8].copy(imagePath = _imagesUri[4])
         people[9] = people[9].copy(imagePath = _imagesUri[10])
         people[10] = people[10].copy(imagePath = _imagesUri[5])
      }
   }

   fun disposeImages() {
      _imagesUri.forEach { imageUrl ->
         logDebug("<disposeImages>", "Url $imageUrl")
         deleteFileOnStorage(imageUrl)
      }
   }
}