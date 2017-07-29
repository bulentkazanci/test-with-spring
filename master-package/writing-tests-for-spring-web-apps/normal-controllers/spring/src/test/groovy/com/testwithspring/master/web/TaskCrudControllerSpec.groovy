package com.testwithspring.master.web

import com.testwithspring.master.UnitTest
import com.testwithspring.master.common.NotFoundException
import com.testwithspring.master.task.TagDTO
import com.testwithspring.master.task.TaskCrudService
import com.testwithspring.master.task.TaskDTO
import com.testwithspring.master.task.TaskListDTO
import com.testwithspring.master.task.TaskResolution
import com.testwithspring.master.task.TaskStatus
import com.testwithspring.master.user.PersonDTO
import org.junit.experimental.categories.Category
import org.springframework.context.support.StaticMessageSource
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import spock.lang.Specification

import java.time.ZonedDateTime

import static com.testwithspring.master.web.WebTestConfig.exceptionResolver
import static com.testwithspring.master.web.WebTestConfig.fixedLocaleResolver
import static com.testwithspring.master.web.WebTestConfig.jspViewResolver
import static org.hamcrest.Matchers.allOf
import static org.hamcrest.Matchers.contains
import static org.hamcrest.Matchers.hasProperty
import static org.hamcrest.Matchers.hasSize
import static org.hamcrest.Matchers.is
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view

@Category(UnitTest.class)
class TaskCrudControllerSpec extends Specification {

    //Task
    private static final ASSIGNEE_ID = 44L
    private static final ASSIGNEE_NAME = 'Anne Assignee'
    private static final CREATION_TIME = ZonedDateTime.now().minusDays(4)
    private static final CREATOR_ID = 99L
    private static final CREATOR_NAME = 'John Doe'
    private static final MODIFICATION_TIME = CREATION_TIME.plusDays(2)
    private static final MODIFIER_ID = 33L
    private static final MODIFIER_NAME = 'Jane Doe'
    private static final TASK_DESCRIPTION = 'description'
    private static final TASK_ID = 1L
    private static final TASK_ID_NOT_FOUND = 99L
    private static final TASK_TITLE = 'title'

    def messageSource = new StaticMessageSource()
    def service = Mock(TaskCrudService)
    def mockMvc = MockMvcBuilders.standaloneSetup(new TaskCrudController(service, messageSource))
            .setHandlerExceptionResolvers(exceptionResolver())
            .setLocaleResolver(fixedLocaleResolver())
            .setViewResolvers(jspViewResolver())
            .build()

    def 'Delete task'() {

        given: 'The message source contains contains the feedback message'
        def final FEEDBACK_MESSAGE_KEY_TASK_DELETED = 'feedback.message.task.deleted'
        def final FEEDBACK_MESSAGE_TASK_DELETED = 'Task deleted'

        messageSource.addMessage(FEEDBACK_MESSAGE_KEY_TASK_DELETED,
                WebTestConfig.LOCALE,
                FEEDBACK_MESSAGE_TASK_DELETED
        )

        def response

        when: 'The deleted task is not found'
        service.delete(TASK_ID_NOT_FOUND) >> { throw new NotFoundException('') }

        and: 'A user deletes a task'
        response = mockMvc.perform(get('/task/{taskId}/delete', TASK_ID_NOT_FOUND))

        then: 'Should return HTTP status code not found'
        response.andExpect(status().isNotFound())

        and: 'Should render the not found view'
        response.andExpect(view().name(WebTestConstants.ErrorView.NOT_FOUND))

        when: 'The found task is deleted and the deleted task is returned returned'
        1 * service.delete(TASK_ID) >> new TaskDTO(id: TASK_ID, title: TASK_TITLE)

        and: 'A user deletes the task'
        response = mockMvc.perform(get('/task/{taskId}/delete', TASK_ID))

        then: 'Should return HTTP status code found'
        response.andExpect(status().isFound())

        and: 'Should redirect the user to the view task list view'
        response.andExpect(view().name(WebTestConstants.RedirectView.SHOW_TASK_LIST))

        and: 'Should add feedback message as a flash attribute'
        response.andExpect(flash().attribute(WebTestConstants.FlashMessageKey.FEEDBACK_MESSAGE,
                FEEDBACK_MESSAGE_TASK_DELETED
        ))
    }

    def 'Show task'() {

        def final CLOSER_ID = 931L
        def final CLOSER_NAME = 'Chris Closer'
        def final TAG_ID = 33L
        def final TAG_NAME = 'testing'

        def response

        when: 'No task is found'
        service.findById(TASK_ID_NOT_FOUND) >> { throw new NotFoundException('') }

        and: 'A user tries to open the view task page'
        response = mockMvc.perform(get('/task/{taskId}', TASK_ID_NOT_FOUND))

        then: 'Should return HTTP status code not found'
        response.andExpect(status().isNotFound())

        and: 'Should render the not found view'
        response.andExpect(view().name(WebTestConstants.ErrorView.NOT_FOUND))

        when: 'A closed task is found and it has one tag'
        service.findById(TASK_ID) >> {
            new TaskDTO(
                    id: TASK_ID,
                    assignee: new PersonDTO(userId: ASSIGNEE_ID, name: ASSIGNEE_NAME),
                    closer: new PersonDTO(userId: CLOSER_ID, name: CLOSER_NAME),
                    creationTime: CREATION_TIME,
                    creator: new PersonDTO(userId: CREATOR_ID, name: CREATOR_NAME),
                    description: TASK_DESCRIPTION,
                    modificationTime: MODIFICATION_TIME,
                    modifier: new PersonDTO(userId: MODIFIER_ID, name: MODIFIER_NAME),
                    resolution: TaskResolution.DONE,
                    status: TaskStatus.CLOSED,
                    tags: [new TagDTO(id: TAG_ID, name: TAG_NAME)],
                    title: TASK_TITLE,
            )
        }

        and: 'A user opens the view task page'
        response = mockMvc.perform(get('/task/{taskId}', TASK_ID))

        then: 'Should return HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should render the view task view'
        response.andExpect(view().name(WebTestConstants.View.VIEW_TASK))

        and: 'Should render the information of the found task'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK, allOf(
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.ASSIGNEE, allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.NAME, is(ASSIGNEE_NAME)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.USER_ID, is(ASSIGNEE_ID))
                )),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.CLOSER, allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.NAME, is(CLOSER_NAME)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.USER_ID, is(CLOSER_ID))
                )),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.CREATION_TIME, is(CREATION_TIME)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.CREATOR, allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.NAME, is(CREATOR_NAME)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.USER_ID, is(CREATOR_ID))
                )),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.ID, is(TASK_ID)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.MODIFICATION_TIME, is(MODIFICATION_TIME)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.MODIFIER, allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.NAME, is(MODIFIER_NAME)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.Person.USER_ID, is(MODIFIER_ID))
                )),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.TITLE, is(TASK_TITLE)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.DESCRIPTION, is(TASK_DESCRIPTION)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.STATUS, is(TaskStatus.CLOSED)),
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.RESOLUTION, is(TaskResolution.DONE))
        )))

        and: 'Should render one tag of the found task'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK,
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.TAGS, hasSize(1))
        ))

        and: 'Should render the tag of the found task'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK,
                hasProperty(WebTestConstants.ModelAttributeProperty.Task.TAGS, contains(
                        allOf(
                                hasProperty(WebTestConstants.ModelAttributeProperty.Tag.ID, is(TAG_ID)),
                                hasProperty(WebTestConstants.ModelAttributeProperty.Tag.NAME, is(TAG_NAME))
                        )
                ))
        ))
    }

    def 'Show task list'() {

        def final FIRST_TASK_ID = 1L
        def final FIRST_TASK_TITLE = 'firstTask'
        def final SECOND_TASK_ID = 33L
        def final SECOND_TASK_TITLE = 'secondTask'

        def response

        when: 'The returned task list is irrelevant'
        1 * service.findAll() >> []

        and: 'A user opens the task list page'
        response = mockMvc.perform(get('/'))

        then: 'Should return the HTTP status code OK'
        response.andExpect(status().isOk())

        and: 'Should render the task list view'
        response.andExpect(view().name(WebTestConstants.View.TASK_LIST))

        when: 'No tasks is found'
        1 * service.findAll() >> []

        and: 'A user opens the task list page'
        response = mockMvc.perform(get('/'))

        then: 'Should show an empty task list'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK_LIST, hasSize(0)))

        when: 'Two tasks is found'
        1 * service.findAll() >> [
                new TaskListDTO(id: FIRST_TASK_ID, title: FIRST_TASK_TITLE, status: TaskStatus.OPEN),
                new TaskListDTO(id: SECOND_TASK_ID, title: SECOND_TASK_TITLE, status: TaskStatus.OPEN)
        ]

        and: 'A user opens the task list page'
        response = mockMvc.perform(get('/'))

        then: 'Should show task list that has two tasks'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK_LIST, hasSize(2)))

        and: 'Should show task list that contains the correct information'
        response.andExpect(model().attribute(WebTestConstants.ModelAttributeName.TASK_LIST, contains(
                allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.ID, is(FIRST_TASK_ID)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.TITLE, is(FIRST_TASK_TITLE)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.STATUS, is(TaskStatus.OPEN))
                ),
                allOf(
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.ID, is(SECOND_TASK_ID)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.TITLE, is(SECOND_TASK_TITLE)),
                        hasProperty(WebTestConstants.ModelAttributeProperty.Task.STATUS, is(TaskStatus.OPEN))
                )
        )))
    }
}